// Serverless function to proxy requests to Google Gemini API securely
// using the GEMINI_API_KEY environment variable configured in Netlify.
exports.handler = async function (event) {
  if (event.httpMethod !== "POST") {
    return {
      statusCode: 405,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ error: "Method Not Allowed" })
    };
  }

  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    return {
      statusCode: 503,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        error: "Kein serverseitiger GEMINI_API_KEY auf Netlify konfiguriert."
      })
    };
  }

  try {
    const payload = JSON.parse(event.body || "{}");
    const prompt = payload.prompt;
    const responseMimeType = payload.responseMimeType;

    if (!prompt) {
      return {
        statusCode: 400,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ error: "Prompt darf nicht leer sein." })
      };
    }

    const body = {
      contents: [{ parts: [{ text: prompt }] }]
    };
    if (responseMimeType) {
      body.generationConfig = { responseMimeType };
    }

    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${encodeURIComponent(apiKey)}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
      }
    );

    const data = await response.json();
    if (!response.ok) {
      return {
        statusCode: response.status,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ error: data.error?.message || "Gemini API Fehler" })
      };
    }

    const text = data.candidates?.[0]?.content?.parts?.[0]?.text || "";
    return {
      statusCode: 200,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ text })
    };
  } catch (err) {
    return {
      statusCode: 500,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ error: err.message || "Interner Serverfehler" })
    };
  }
};
