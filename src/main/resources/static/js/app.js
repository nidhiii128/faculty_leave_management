// Small enhancements only; every page works without JavaScript.
document.addEventListener('DOMContentLoaded', function () {
  var toggle = document.getElementById('menu-toggle');
  var nav = document.getElementById('main-nav');
  if (toggle && nav) {
    toggle.addEventListener('click', function () {
      nav.classList.toggle('open');
    });
  }

  // Highlight the document upload when sick leave is chosen
  var type = document.getElementById('leaveType');
  var box = document.getElementById('upload-box');
  if (type && box) {
    var update = function () {
      if (type.value === 'SICK') {
        box.classList.add('highlight');
      } else {
        box.classList.remove('highlight');
      }
    };
    type.addEventListener('change', update);
    update();
  }
});
