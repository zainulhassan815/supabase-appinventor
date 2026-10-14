export default {
  async fetch(req: Request) {
    const status = Number(req.headers.get("x-status") ?? 200);
    const body = await req.text();
    if (status !== 200) {
      return new Response("requested failure", { status });
    }
    return Response.json({
      method: req.method,
      test: req.headers.get("x-test"),
      region: req.headers.get("x-region"),
      contentType: req.headers.get("content-type"),
      body,
    });
  },
};
