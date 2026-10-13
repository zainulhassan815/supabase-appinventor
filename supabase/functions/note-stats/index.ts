export default {
  async fetch(req: Request) {
    const { notes }: { notes: string[] } = await req.json();
    const words = notes.reduce((total, note) => total + note.split(/\s+/).filter(Boolean).length, 0);
    return Response.json({ count: notes.length, words });
  },
};
