export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method !== "GET" && request.method !== "HEAD") {
      return new Response("Méthode non autorisée", {
        status: 405,
        headers: { Allow: "GET, HEAD" },
      });
    }

    const response = await env.ASSETS.fetch(request);
    if (response.status !== 404) return response;

    return new Response("Fichier introuvable", {
      status: 404,
      headers: { "Content-Type": "text/plain; charset=utf-8" },
    });
  },
};
