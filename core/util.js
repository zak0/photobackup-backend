module.exports = {
    /**
     * Converts milliseconds into the datetime format used in EXIF meta data.
     * 
     * 
     * @param {number} millis 
     */
    millisToExifTimeStamp: function(millis) {
        let date = new Date(millis)

        const format = new Intl.DateTimeFormat('en',
            { year: 'numeric',
              month: '2-digit',
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit',
              second:'2-digit'}) 

        var [{ value: mo },,
             { value: da },,
             { value: ye },,
             { value: ho },,
             { value: mi },,
             { value: se }] = format.formatToParts(date)

        return `${ye}:${mo}:${da} ${ho}:${mi}:${se}`
    }
}
