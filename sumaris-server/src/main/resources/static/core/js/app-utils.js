// Workaround to add Math.trunc() if not present - fix #144
if (Math && typeof Math.trunc !== 'function') {
    console.debug("[utils] Adding Math.trunc() -> was missing on this platform");
    Math.trunc = (number) => {
        return parseInt((number - 0.5).toFixed());
    };
}

// Workaround to add "".format() if not present
if (typeof String.prototype.format !== 'function') {
    console.debug("[utils] Adding String.prototype.format() -> was missing on this platform");
    String.prototype.format = function() {
        const args = arguments;
        return this.replace(/{(\d+)}/g, function(match, number) {
            return typeof args[number] != 'undefined' ? args[number] : match;
        });
    };
}
