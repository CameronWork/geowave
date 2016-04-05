var filter;
var map;
var markers = [];
var queryStart;

function initMap() {
    map = new google.maps.Map(document.getElementById('map'), {
        center: {
            lat: -37.8136,
            lng: 144.9631
        },
        zoom: 8
    });

    map.addListener('idle', function (e) {
        var bounds = map.getBounds();
        var ne = bounds.getNorthEast();
        var north = ne.lat();
        var east = ne.lng();
        var sw = bounds.getSouthWest();
        var south = sw.lat();
        var west = sw.lng();
        queryStr = north + "," + west + ","
        + north + "," + east + ","
        + south + "," + east + ","
        + south + "," + west;
        search(queryStr);
    });
}

function clearFilter() {
    if (filter != null) {
        filter.setMap(null);
    }
    filter = null;
}

function circleFilter() {

    var rad = 10000000 / (1 << map.zoom);
    clearFilter();
    filter = new google.maps.Circle({
        strokeColor: '#FF0000',
        strokeOpacity: 0.8,
        strokeWeight: 2,
        fillColor: '#FF0000',
        fillOpacity: 0.35,
        map: map,
        center: map.center,
        radius: rad,
        editable: true,
        draggable: true
    });
    query();
    filter.addListener('bounds_changed', query);
}

function recFilter() {
    var bounds = map.getBounds();
    var width = (bounds.R.j - bounds.R.R) / 8;
    var height = (bounds.j.R - bounds.j.j) / 8;

    var bounds = new google.maps.LatLngBounds({
        lat: map.center.lat() - width,
        lng: map.center.lng() - height
    }, {
        lat: map.center.lat() + width,
        lng: map.center.lng() + height
    });

    clearFilter();
    filter = new google.maps.Rectangle({
        strokeColor: '#FF0000',
        strokeOpacity: 0.8,
        strokeWeight: 2,
        fillColor: '#FF0000',
        fillOpacity: 0.35,
        map: map,
        bounds: bounds,
        editable: true,
        draggable: true
    });
    query();
    filter.addListener('bounds_changed', query);
}

function polyFilter() {
    var bounds = map.getBounds();
    var width = (bounds.R.j - bounds.R.R) / 8;
    var height = (bounds.j.R - bounds.j.j) / 8;

    var coords = [{
        lat: map.center.lat() + height / 4,
        lng: map.center.lng()
    }, {
        lat: map.center.lat() - height / 9,
        lng: map.center.lng() - width
    }, {
        lat: map.center.lat() - height / 9,
        lng: map.center.lng() + width
    }];
    clearFilter();
    filter = new google.maps.Polygon({
        map: map,
        paths: coords,
        strokeColor: '#FF0000',
        strokeOpacity: 0.8,
        strokeWeight: 2,
        fillColor: '#FF0000',
        fillOpacity: 0.35,
        draggable: true,
        geodesic: true,
        editable: true
    });
    google.maps.event.addListener(filter, 'rightclick', function (e) {
        // Check if click was on a vertex control point
        if (e.vertex !== undefined && filter.getPath().getArray().length > 3) {
            filter.getPath().removeAt(e.vertex);
        }
    });
    query();
    filter.addListener('dragend', query);
    filter.getPath().addListener('set_at', query);
    filter.getPath().addListener('insert_at', query);
    filter.getPath().addListener('remove_at', query);

}

function query() {
    var query = "";
    if (filter instanceof google.maps.Circle) {
        var center = filter.getCenter();
        var radius = filter.getRadius();
        query = center.lat() + "," + center.lng() + "," + radius/1000;
    } else if (filter instanceof google.maps.Rectangle) {
        var ne = filter.getBounds().getNorthEast();
        var north = ne.lat();
        var east = ne.lng();
        var sw = filter.getBounds().getSouthWest();
        var south = sw.lat();
        var west = sw.lng();
        query = north + "," + west + ","
        + north + "," + east + ","
        + south + "," + east + ","
        + south + "," + west;


    } else if (filter instanceof google.maps.Polygon) {
        var array = filter.getPath().getArray();
        var output = []
        for (index in array) {
            output.push(array[index].lat());
            output.push(array[index].lng());
        }
        query = output.join(",");
    }
    $("#query").val(query);
    return query;
}

function clearPoints() {
    for (index in markers) {
        markers[index].setMap(null)
    }
}

function drawPoints(points) {
    var icon = {
        path: google.maps.SymbolPath.CIRCLE,
        scale: 2
    };
    clearPoints();
    var points = JSON.parse(points);
    var keys = Object.keys(points);
    var skip = Math.round(keys.length/100);
    if (skip == 0) {
        skip = 1;
    }
    var i=0;
    for (index in keys) {
        var id = keys[index];
        i = (i+1) % skip;
        if (i==0) {
            if (id in markers) {
                markers[id].setMap(map);
            } else {
                var parts = points[id].pointCoords.split(" ");
                var marker = new google.maps.Marker({
                    position: {lat: parseFloat(parts[0]), lng:  parseFloat(parts[1])},
                    map: map,
                    title: points[id].name,
                    icon: icon
                });
                markers[id] = marker;
            }

        }
    }

}

function searchButton() {
    search($("#query").val());
}

function search(query) {
    queryStart = new Date().getTime();
    startTime = queryStart-(5*60*1000);
    endTime = startTime;
    $.ajax({url: "/geosearch?values="+query+"&startTime="+startTime+"&endTime="+endTime, success: function(result){
        //$("#results").html(result);
        drawPoints(result);
        var end = new Date().getTime();
        $("#results").html("Total Query Time was:"+(end-queryStart));
    }});

}
