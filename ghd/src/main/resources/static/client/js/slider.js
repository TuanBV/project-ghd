(function () {
  function parseSliderImages(rawValue) {
    if (!rawValue) return [];

    var value = rawValue.trim();
    if (!value || value === "[]" || value === "null") return [];

    try {
      var parsed = JSON.parse(value);
      return Array.isArray(parsed) ? parsed : [];
    } catch (error) {
      return value
        .replace(/^\[/, "")
        .replace(/\]$/, "")
        .split(",")
        .map(function (item) {
          return item.trim();
        })
        .filter(Boolean);
    }
  }

  function registerSliderComponent() {
    if (window.sliderComponentLoaded || !window.Alpine) return;

    window.Alpine.data("slider", function () {
      return {
        currentIndex: 1,
        images: [],
        pause: false,
        touchStartX: 0,
        touchEndX: 0,
        threshold: 50,

        init: function () {
          this.images = parseSliderImages(this.$el.getAttribute("data-slider-images"));

          if (this.images.length > 1) {
            var self = this;
            window.setTimeout(function () {
              window.setInterval(function () {
                if (!self.pause) self.next();
              }, 5000);
            }, 15000);
          }
        },

        next: function () {
          this.currentIndex =
            this.currentIndex >= this.images.length ? 1 : this.currentIndex + 1;
        },

        prev: function () {
          this.currentIndex =
            this.currentIndex <= 1 ? this.images.length : this.currentIndex - 1;
        },

        handleTouchStart: function (event) {
          this.touchStartX = event.changedTouches[0].screenX;
        },

        handleTouchEnd: function (event) {
          this.touchEndX = event.changedTouches[0].screenX;
          this.handleSwipe();
        },

        handleSwipe: function () {
          var distance = this.touchStartX - this.touchEndX;
          if (Math.abs(distance) <= this.threshold) return;

          if (distance > 0) {
            this.next();
          } else {
            this.prev();
          }
        },
      };
    });

    window.sliderComponentLoaded = true;
  }

  document.addEventListener("alpine:init", registerSliderComponent);
  registerSliderComponent();
})();
