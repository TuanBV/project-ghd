const allSideMenu = document.querySelectorAll('#sidebar .side-menu.top li a');

allSideMenu.forEach(item => {
  const li = item.parentElement;

  item.addEventListener('click', function () {
    const targetUrl = new URL(item.href, window.location.href);
    if (targetUrl.pathname !== window.location.pathname) {
      return;
    }

    allSideMenu.forEach(i => {
      i.parentElement.classList.remove('active');
    })
    li.classList.add('active');
  })
});

// TOGGLE SIDEBAR
const sidebar = document.getElementById('sidebar');
const narrowSidebarQuery = window.matchMedia('(max-width: 576px)');


function adjustSidebar() {
  if (!sidebar) return;
  if (narrowSidebarQuery.matches) {
    if (!sidebar.classList.contains('hide')) {
      sidebar.classList.add('hide');
    }
    sidebar.classList.remove('show');
  } else {
    if (sidebar.classList.contains('hide')) {
      sidebar.classList.remove('hide');
    }
    sidebar.classList.add('show');
  }
}

window.addEventListener('load', adjustSidebar);
if (narrowSidebarQuery.addEventListener) {
  narrowSidebarQuery.addEventListener('change', adjustSidebar);
} else {
  narrowSidebarQuery.addListener(adjustSidebar);
}
