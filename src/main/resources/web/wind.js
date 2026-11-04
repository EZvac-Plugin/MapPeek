/*
 * MapPeek 0.2.0: EZvac's wind as moving particles, like a weather site's wind
 * map, on squaremap and BlueMap. MapPeek writes the wind on its grid every
 * wind refresh; this draws particles drifting along it, coloured by how
 * strong it blows, while the viewer has "Wind flow" ticked. It all runs in the
 * viewer's browser, and is kept light: one coordinate transform per frame,
 * the wind in flat arrays, the streaks drawn in four batches, no shadows.
 *
 * On squaremap, which keeps its Leaflet map to itself, this script runs
 * before squaremap's own and catches the map as Leaflet creates it. On
 * BlueMap, which loads it as an addon script, it reads BlueMap's camera.
 */
(function () {
    'use strict';
    var LAYER = 'Wind flow';
    var MAX_AGE = 120;

    // ---- the wind on its grid, as flat arrays ----

    function Field(data) {
        var s = data.spacing, minX = Infinity, minZ = Infinity, maxX = -Infinity, maxZ = -Infinity;
        data.points.forEach(function (p) {
            var gx = Math.round(p[0] / s), gz = Math.round(p[1] / s);
            if (gx < minX) minX = gx; if (gx > maxX) maxX = gx;
            if (gz < minZ) minZ = gz; if (gz > maxZ) maxZ = gz;
        });
        this.spacing = s;
        this.minX = minX; this.minZ = minZ;
        this.width = maxX - minX + 1; this.height = maxZ - minZ + 1;
        var size = Math.max(0, this.width * this.height);
        this.u = new Float32Array(size);
        this.v = new Float32Array(size);
        this.level = new Int8Array(size).fill(-1);
        this.cells = [];
        var self = this;
        data.points.forEach(function (p) {
            var i = (Math.round(p[0] / s) - minX) * self.height + (Math.round(p[1] / s) - minZ);
            self.u[i] = p[2]; self.v[i] = p[3]; self.level[i] = p[4];
            self.cells.push(i);
        });
        this.colors = data.colors;
        this.y = data.y || 64;
    }

    Field.prototype.at = function (gx, gz) {
        gx -= this.minX; gz -= this.minZ;
        if (gx < 0 || gz < 0 || gx >= this.width || gz >= this.height) return -1;
        var i = gx * this.height + gz;
        return this.level[i] < 0 ? -1 : i;
    };

    // The wind at a spot, blended from the four grid points around it, into out: [east, south, level].
    Field.prototype.sample = function (x, z, out) {
        var s = this.spacing, fx = x / s, fz = z / s, gx = Math.floor(fx), gz = Math.floor(fz);
        fx -= gx; fz -= gz;
        var u = 0, v = 0, weight = 0, best = -1, level = 0;
        for (var k = 0; k < 4; k++) {
            var dx = k & 1, dz = k >> 1, i = this.at(gx + dx, gz + dz);
            if (i < 0) continue;
            var w = (dx ? fx : 1 - fx) * (dz ? fz : 1 - fz);
            u += this.u[i] * w; v += this.v[i] * w; weight += w;
            if (w > best) { best = w; level = this.level[i]; }
        }
        if (weight < 0.2) return false;
        out[0] = u / weight; out[1] = v / weight; out[2] = level;
        return true;
    };

    // A random spot in one of the grid's cells, so particles spread evenly over the wind there is.
    Field.prototype.spot = function (inView) {
        for (var tries = 0; tries < 12; tries++) {
            var i = this.cells[(Math.random() * this.cells.length) | 0];
            var gx = Math.floor(i / this.height) + this.minX, gz = (i % this.height) + this.minZ;
            var x = (gx + Math.random() - 0.5) * this.spacing, z = (gz + Math.random() - 0.5) * this.spacing;
            if (!inView || inView(x, z)) return [x, z];
        }
        return null;
    };

    // ---- the particles, the same for both maps ----

    /*
     * view(): this frame's view, or null to draw nothing: {width, height, project(x, z, out) -> bool,
     * perBlock, inView(x, z), moved}. Its project writes screen x and y into out.
     */
    function Animation(parent, fieldUrl, view, shown) {
        var canvas = document.createElement('canvas');
        canvas.style.cssText = 'position:absolute;top:0;left:0;width:100%;height:100%;pointer-events:none;z-index:450';
        parent.appendChild(canvas);
        var context = canvas.getContext('2d');
        var field = null, particles = [], lastFetch = 0, url = null, wind = [0, 0, 0], a = [0, 0], b = [0, 0];
        var batches = [[], [], [], []];

        function fetchField() {
            var next = fieldUrl();
            lastFetch = Date.now();
            if (!next) { field = null; return; }
            fetch(next + (next.indexOf('?') < 0 ? '?' : '&') + lastFetch).then(function (response) {
                return response.ok ? response.json() : null;
            }).then(function (data) {
                field = data && data.points && data.points.length ? new Field(data) : null;
                if (next !== url) { url = next; particles = []; }
            }).catch(function () { field = null; });
        }

        function respawn(p, current) {
            var spot = field.spot(current.inView);
            if (!spot) { p.dead = true; return; }
            p.x = spot[0]; p.z = spot[1]; p.age = (Math.random() * MAX_AGE) | 0; p.dead = false;
        }

        // How long drawing takes, for anyone checking it's light: window.mappeekWind.frameMs.
        var stats = window.mappeekWind = window.mappeekWind || { frameMs: 0, particles: 0 };

        /*
         * How many particles: one per so many pixels of wind actually on screen, not of
         * the whole screen, so zooming out thins them instead of piling them up. Counted
         * again only when the view moves.
         */
        var crowdSize = 0, crowdField = null;
        function crowd(current) {
            if (!current.moved && crowdField === field && crowdSize) return crowdSize;
            var inView = 0, cell = field.spacing * Math.max(1e-6, current.perBlock);
            for (var k = 0; k < field.cells.length; k++) {
                var i = field.cells[k];
                var x = (Math.floor(i / field.height) + field.minX) * field.spacing;
                var z = (i % field.height + field.minZ) * field.spacing;
                if (!current.inView || current.inView(x, z)) inView++;
            }
            var pixels = Math.min(current.width * current.height, inView * cell * cell);
            // Further out, fewer still: a zoomed-out map shows the pattern, not every gust.
            var zoomedOut = Math.min(1, Math.max(0.35, current.perBlock * 2));
            crowdField = field;
            crowdSize = Math.max(150, Math.min(1500, Math.round(pixels / 1400 * zoomedOut)));
            return crowdSize;
        }

        function frame() {
            requestAnimationFrame(frame);
            var started = performance.now();
            draw();
            if (particles.length) {
                stats.frameMs = stats.frameMs * 0.9 + (performance.now() - started) * 0.1;
                stats.particles = particles.length;
            }
        }

        function draw() {
            if (Date.now() - lastFetch > 15000) fetchField();
            var current = shown() && field ? view(field) : null;
            if (!current) {
                if (particles.length) { particles = []; context.clearRect(0, 0, canvas.width, canvas.height); }
                return;
            }
            if (canvas.width !== current.width || canvas.height !== current.height) {
                canvas.width = current.width; canvas.height = current.height;
            }
            var wanted = crowd(current);
            while (particles.length < wanted) { var fresh = {}; respawn(fresh, current); particles.push(fresh); }
            if (particles.length > wanted) particles.length = wanted;
            if (current.moved) {
                // The view moved: no trails while it does.
                context.clearRect(0, 0, canvas.width, canvas.height);
            } else {
                context.globalCompositeOperation = 'destination-in';
                context.fillStyle = 'rgba(0,0,0,0.94)';
                context.fillRect(0, 0, canvas.width, canvas.height);
                context.globalCompositeOperation = 'source-over';
            }
            for (var c = 0; c < 4; c++) batches[c].length = 0;
            var perBlock = Math.max(1e-6, current.perBlock);
            for (var n = 0; n < particles.length; n++) {
                var p = particles[n];
                if (p.dead || p.age++ > MAX_AGE || !field.sample(p.x, p.z, wind)) { respawn(p, current); continue; }
                var speed = Math.sqrt(wind[0] * wind[0] + wind[1] * wind[1]);
                if (speed < 0.05) { p.age += 3; continue; }
                // Gentle winds crawl, gales race: up to three pixels a frame, at any zoom.
                var step = Math.min(3.0, 0.35 + speed * 0.13) / perBlock / speed;
                var fromX = p.x, fromZ = p.z;
                p.x += wind[0] * step; p.z += wind[1] * step;
                if (!current.project(fromX, fromZ, a) || !current.project(p.x, p.z, b)) continue;
                batches[wind[2] & 3].push(a[0], a[1], b[0], b[1]);
            }
            context.lineWidth = perBlock < 0.5 ? 1.5 : 2;
            context.lineCap = 'round';
            for (var level = 0; level < 4; level++) {
                var lines = batches[level];
                if (!lines.length) continue;
                context.strokeStyle = field.colors[level] || '#ffffff';
                context.beginPath();
                for (var j = 0; j < lines.length; j += 4) { context.moveTo(lines[j], lines[j + 1]); context.lineTo(lines[j + 2], lines[j + 3]); }
                context.stroke();
            }
        }

        fetchField();
        requestAnimationFrame(frame);
    }

    // ---- squaremap ----

    var leaflet;
    Object.defineProperty(window, 'L', {
        configurable: true,
        get: function () { return leaflet; },
        set: function (value) {
            leaflet = value;
            try { value.Map.addInitHook(function () { squaremap(this); }); } catch (ignored) { /* not Leaflet */ }
        }
    });

    function squaremap(map) {
        if (map._mappeekWind) return;
        map._mappeekWind = true;
        var on = false, moving = false, world = null, checked = 0;
        // squaremap places blocks by its tiles' own top zoom, not the map's: it can zoom in further
        // ("extra" levels), and using the map's would shrink everything toward 0, 0.
        var nativeZoom = {};
        function tileZoom(name) {
            if (nativeZoom[name] === undefined) {
                nativeZoom[name] = null;
                fetch('tiles/' + name + '/settings.json').then(function (r) { return r.ok ? r.json() : null; })
                    .then(function (data) { nativeZoom[name] = data && data.zoom ? data.zoom.max : map.getMaxZoom(); })
                    .catch(function () { nativeZoom[name] = map.getMaxZoom(); });
            }
            return nativeZoom[name];
        }
        map.on('overlayadd', function (e) { if (e.name === LAYER) on = true; });
        map.on('overlayremove', function (e) { if (e.name === LAYER) on = false; });
        map.on('movestart zoomstart', function () { moving = true; });
        map.on('moveend zoomend', function () { moving = false; });

        // The world on show: squaremap names it in the page's title; its tiles are blobs and don't say.
        function currentWorld() {
            var match = /\s-\s([a-z0-9_.-]+:[a-z0-9_./-]+)\s*$/i.exec(document.title || '');
            return match ? match[1].replace(':', '_') : null;
        }

        function shown() {
            // Ticked before this script saw it, or from a saved state: look once every two seconds.
            var now = Date.now();
            if (now - checked > 2000) {
                checked = now;
                world = currentWorld();
                var labels = map.getContainer().querySelectorAll('.leaflet-control-layers-overlays label');
                for (var i = 0; i < labels.length; i++)
                    if (labels[i].textContent.trim() === LAYER) on = labels[i].querySelector('input').checked;
            }
            return on && !!world && tileZoom(world) !== null;
        }

        map.whenReady(function () {
            var lastZoom = null, lastCenter = null;
            Animation(map.getContainer(), function () { return world ? 'mappeek/wind-' + world + '.json' : null; },
                function () {
                    var size = map.getSize(), zoom = map.getZoom(), max = tileZoom(world);
                    // squaremap's blocks map to the screen by a scale and an offset: work them out once a frame.
                    var origin = map.latLngToContainerPoint(map.unproject([0, 0], max));
                    var far = map.latLngToContainerPoint(map.unproject([1024, 1024], max));
                    var kx = (far.x - origin.x) / 1024, kz = (far.y - origin.y) / 1024;
                    var center = map.getCenter(), moved = moving || zoom !== lastZoom || !lastCenter || !center.equals(lastCenter);
                    lastZoom = zoom; lastCenter = center;
                    var minX = -origin.x / kx, minZ = -origin.y / kz, maxX = (size.x - origin.x) / kx, maxZ = (size.y - origin.y) / kz;
                    // Kept for anyone checking particles line up with the map: window.mappeekWind.toScreen(x, z).
                    window.mappeekWind.toScreen = function (x, z) { return [origin.x + x * kx, origin.y + z * kz]; };
                    return {
                        width: size.x, height: size.y, perBlock: kx, moved: moved,
                        project: function (x, z, out) { out[0] = origin.x + x * kx; out[1] = origin.y + z * kz; return true; },
                        inView: function (x, z) { return x >= minX && x <= maxX && z >= minZ && z <= maxZ; }
                    };
                }, shown);
        });
    }

    // ---- BlueMap ----

    function bluemap(app) {
        var viewer = app.mapViewer, lastMatrix = new Float32Array(16), m = new Float32Array(16);

        function set() {
            var sets = viewer.markers && viewer.markers.markerSets;
            return sets && sets.get ? sets.get('mappeek-wind-flow') : null;
        }

        function shown() {
            var flow = set();
            return !!(flow && flow.visible && viewer.map);
        }

        // This frame's camera, as one matrix: projection × view.
        function matrix() {
            var camera = viewer.camera, p = camera.projectionMatrix.elements, v = camera.matrixWorldInverse.elements;
            for (var row = 0; row < 4; row++)
                for (var col = 0; col < 4; col++)
                    m[col * 4 + row] = p[row] * v[col * 4] + p[4 + row] * v[col * 4 + 1] + p[8 + row] * v[col * 4 + 2] + p[12 + row] * v[col * 4 + 3];
            var moved = false;
            for (var i = 0; i < 16; i++) if (Math.abs(m[i] - lastMatrix[i]) > 1e-9) { moved = true; break; }
            lastMatrix.set(m);
            return moved;
        }

        var root = viewer.rootElement;
        if (getComputedStyle(root).position === 'static') root.style.position = 'relative';
        Animation(root, function () {
            return viewer.map && viewer.map.data ? 'maps/' + viewer.map.data.id + '/assets/mappeek/wind.json' : null;
        }, function (field) {
            var width = root.clientWidth, height = root.clientHeight;
            if (!width || !height) return null;
            // Particles drift at sea level, which MapPeek puts in the wind file.
            var moved = matrix(), y = field.y;
            function project(x, z, out) {
                var w = m[3] * x + m[7] * y + m[11] * z + m[15];
                if (w <= 0.01) return false;
                out[0] = ((m[0] * x + m[4] * y + m[8] * z + m[12]) / w + 1) * 0.5 * width;
                out[1] = (1 - (m[1] * x + m[5] * y + m[9] * z + m[13]) / w) * 0.5 * height;
                return out[0] > -50 && out[0] < width + 50 && out[1] > -50 && out[1] < height + 50;
            }
            // How many pixels a block is near the middle of the view, so particles keep their pace.
            var position = viewer.controlsManager ? viewer.controlsManager.position : null;
            var cx = position ? position.x : 0, cz = position ? position.z : 0, a = [0, 0], b = [0, 0];
            var perBlock = project(cx, cz, a) && project(cx + 16, cz, b) ? Math.hypot(b[0] - a[0], b[1] - a[1]) / 16 : 1;
            var reach = viewer.controlsManager ? Math.max(256, viewer.controlsManager.distance * 2.5) : 2048;
            return {
                width: width, height: height, perBlock: perBlock, moved: moved, project: project,
                inView: function (x, z) { return Math.abs(x - cx) < reach && Math.abs(z - cz) < reach && project(x, z, a); }
            };
        }, shown);
    }

    // BlueMap loads addon scripts once its app is up; squaremap never defines window.bluemap.
    (function waitForBlueMap(tries) {
        if (window.bluemap && window.bluemap.mapViewer) return bluemap(window.bluemap);
        if (tries > 0) setTimeout(function () { waitForBlueMap(tries - 1); }, 250);
    })(120);
})();
