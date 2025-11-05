// src/main/resources/static/js/masonry.js
document.addEventListener("DOMContentLoaded", function () {
  // Simple masonry layout using CSS Grid
  const gallery = document.getElementById("photo-gallery");

  // Add CSS for masonry grid
  const style = document.createElement("style");
  style.textContent = `
        .masonry-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
            grid-gap: 15px;
            padding: 15px;
        }
        
        .masonry-item {
            break-inside: avoid;
            margin-bottom: 15px;
        }
        
        @media (max-width: 768px) {
            .masonry-grid {
                grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
                grid-gap: 10px;
                padding: 10px;
            }
        }
        
        @media (max-width: 480px) {
            .masonry-grid {
                grid-template-columns: 1fr;
                grid-gap: 10px;
                padding: 5px;
            }
        }
    `;

  document.head.appendChild(style);

  // Handle image loading errors
  const images = document.querySelectorAll(".gallery-image");
  images.forEach((img) => {
    img.addEventListener("error", function () {
      this.src = "/images/placeholder.jpg";
    });
  });

  // Auto-refresh gallery every 30 seconds (optional)
  // setInterval(() => {
  //     location.reload();
  // }, 30000);
});
