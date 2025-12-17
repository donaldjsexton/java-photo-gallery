const fileInput = document.getElementById("fileInput");
const duplicateModeSelect = document.getElementById("duplicateMode");

if (fileInput && duplicateModeSelect) {
  fileInput.addEventListener("change", function (e) {
    const files = Array.from(e.target.files);
    const mode = duplicateModeSelect.value;

    uploadAll(files, mode);
  });
}

function getCsrf() {
  const token =
    document.querySelector('meta[name="_csrf"]')?.getAttribute("content") ||
    null;
  const header =
    document
      .querySelector('meta[name="_csrf_header"]')
      ?.getAttribute("content") || "X-CSRF-TOKEN";
  return { token, header };
}

async function uploadAll(files, mode) {
  const { token, header } = getCsrf();
  if (!token) {
    alert("Missing CSRF token; refresh and try again.");
    return;
  }

  for (const file of files) {
    const formData = new FormData();
    formData.append("file", file);

    try {
      const response = await fetch(`/api/photos?onDuplicate=${mode}`, {
        method: "POST",
        body: formData,
        credentials: "same-origin",
        headers: {
          [header]: token,
        },
      });

      if (!response.ok) {
        const text = await response.text();
        alert("Upload failed: " + text);
        return;
      }

      const photo = await response.json();
      console.log("Uploaded:", photo);
    } catch (err) {
      alert("Network error or server unavailable");
      return;
    }
  }

  location.reload();
}
