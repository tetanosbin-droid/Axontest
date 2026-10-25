const FILES = {
  "axon-projet-complet.zip": {
    parts: ["downloads/axon-projet-complet.zip"],
    downloadName: "axon-projet-complet.zip",
    size: 55206,
  },
  "axon-apk.zip": {
    parts: ["downloads/axon-apk.zip"],
    downloadName: "axon-apk.zip",
    size: 11309259,
  },
};

const text = (body, status = 200, headers = {}) =>
  new Response(body, { status, headers: { "Content-Type": "text/plain; charset=utf-8", ...headers } });

function joinBodies(objects) {
  let index = 0;
  let reader = null;
  return new ReadableStream({
    async pull(controller) {
      try {
        while (index < objects.length) {
          if (!reader) reader = objects[index].body.getReader();
          const result = await reader.read();
          if (!result.done) {
            controller.enqueue(result.value);
            return;
          }
          reader.releaseLock();
          reader = null;
          index++;
        }
        controller.close();
      } catch (error) {
        controller.error(error);
      }
    },
    async cancel(reason) {
      if (reader) await reader.cancel(reason);
      for (let i = index + (reader ? 1 : 0); i < objects.length; i++) {
        await objects[i].body.cancel(reason);
      }
    },
  });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method !== "GET" && request.method !== "HEAD") {
      return text("Méthode non autorisée", 405, { Allow: "GET, HEAD" });
    }

    if (url.pathname === "/" || url.pathname === "/index.html") {
      return env.ASSETS.fetch(new Request(new URL("/index.html", url), request));
    }

    const prefix = "/download/";
    if (!url.pathname.startsWith(prefix)) return text("Fichier introuvable", 404);
    const name = url.pathname.slice(prefix.length);
    const file = FILES[name];
    if (!file || url.pathname !== `${prefix}${name}`) return text("Fichier introuvable", 404);

    try {
      const objects = [];
      for (const key of file.parts) {
        const object = await env.FILES.get(key);
        if (!object) return text("Fichier introuvable", 404);
        objects.push(object);
      }
      const headers = new Headers({
        "Content-Type": "application/zip",
        "Content-Disposition": `attachment; filename=\"${file.downloadName}\"`,
        "Cache-Control": "public, max-age=3600",
        "X-Content-Type-Options": "nosniff",
        "Content-Length": String(file.size),
      });
      if (objects.length === 1 && objects[0].httpEtag) headers.set("ETag", objects[0].httpEtag);
      if (request.method === "HEAD") {
        await Promise.all(objects.map((object) => object.body.cancel()));
        return new Response(null, { headers });
      }
      return new Response(joinBodies(objects), { headers });
    } catch {
      return text("Le fichier est temporairement indisponible", 503);
    }
  },
};
