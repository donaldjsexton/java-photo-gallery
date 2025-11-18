document.getElementById("fileInput").addEventListener("change", function (e) {
  const files = Array.from(e.target.files);
  const mode = document.getElementById("duplicateMode").value;

  files.forEach((file) => uploadFile(file, mode));
});

async function uploadFile(file, mode) {
  const formData = new FormData();
  formData.append("file", file);

  try {
    const response = await fetch(`/api/photos?onDuplicate=${mode}`, {
      method: "POST",
      body: formData,
    });

    if (!response.ok) {
      const text = await response.text();
      alert("Upload failed: " + text);
      return;
    }

    // You return a Photo JSON object, so parse it:
    const photo = await response.json();

    console.log("Uploaded:", photo);

    // Reload after each file upload completes (or delay until last)
    location.reload();
  } catch (err) {
    alert("Network error or server unavailable");
  }
}
