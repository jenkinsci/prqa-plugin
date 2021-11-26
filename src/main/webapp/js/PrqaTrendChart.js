
var renderChart = function(chartData, canvasId) {
    var ctx = document.getElementById(canvasId).getContext("2d");

/*
    // Useful when debugging
    console.log(ctx);
    console.log(canvasId);
    console.log(chartData);
*/

    var yLabel = (canvasId.includes("Messages") ? 'Messages' : '% Compliance');

    try {
        var lineChart = new Chart(ctx, {
            type: 'line',
            data: chartData,
            options: {
                responsive: true,
                legend: {
                    display: true,
                    position: 'bottom'
                },
                title: {
                    display: true,
                    text: canvasId
                },
                tooltips: {
                    mode: 'index',
                    intersect: false,
                },
                hover: {
                    mode: 'nearest',
                    intersect: true
                },
                scales: {
                    xAxes: [{
                        display: true,
                        scaleLabel: {
                            display: true,
                            labelString: 'Build Number'
                        }
                    }],
                    yAxes: [{
                        display: true,
                        scaleLabel: {
                            display: true,
                            labelString: yLabel
                        },
                         ticks: {
                            beginAtZero: true,
                             callback: function(value) {if (value % 1 === 0) {return value;}}
                         }
                    }]
                }
            }
        });
    }
    catch (err) {
        console.log(err);
    }
}
