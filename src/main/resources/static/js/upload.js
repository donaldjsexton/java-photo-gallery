document.getElementById("fileInput").addEventListener("change", function (e) {
  Array.from(e.target.files).forEach(uploadFile);
});

async function uploadFile(file) {
  const formData = new FormData();
  formData.append("file", file);

  try {
    const response = await fetch("/api/upload", {
      method: "POST",
      body: formData,
    });
    const result = await response.json();

    if (result.success) {
      location.reload(); // Refresh gallery
    } else {
      alert("Upload failed: " + result.error);
    }
  } catch (error) {
    alert("Network error");
  }
}
