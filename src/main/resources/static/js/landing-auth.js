(() => {
  const dialog = document.getElementById("auth-modal");
  const openButtons = document.querySelectorAll("[data-auth-open]");

  for (const button of openButtons) {
    button.addEventListener("click", () => {
      if (dialog && typeof dialog.showModal === "function") {
        dialog.showModal();
        return;
      }

      window.location.assign("/oauth2/authorization/keycloak");
    });
  }

  if (dialog) {
    dialog.addEventListener("click", (event) => {
      if (event.target === dialog) {
        dialog.close();
      }
    });
  }

  document.addEventListener("click", (event) => {
    const openDropdowns = document.querySelectorAll("details.dropdown[open]");
    for (const dropdown of openDropdowns) {
      if (!dropdown.contains(event.target)) {
        dropdown.removeAttribute("open");
      }
    }
  });

  document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape") {
      return;
    }

    const openDropdowns = document.querySelectorAll("details.dropdown[open]");
    for (const dropdown of openDropdowns) {
      dropdown.removeAttribute("open");
    }
  });
})();

