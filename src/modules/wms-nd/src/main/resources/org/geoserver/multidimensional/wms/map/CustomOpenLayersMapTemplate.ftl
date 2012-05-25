<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
	<title>OpenLayers map preview</title>
	<!-- Import OL CSS, auto import does not work with our minified OL.js build -->
	<link rel="stylesheet" type="text/css" href="${baseUrl}/openlayers/theme/default/style.css"/>
	<!-- Basic CSS definitions -->
	<style type="text/css">
		/* General settings */
		body {
			font-family: Verdana, Geneva, Arial, Helvetica, sans-serif;
			font-size: small;
		}
		/* Toolbar styles */
		#toolbar {
			position: relative;
			padding-bottom: 0.5em;
			display: none;
			clear: both;
		}

		#toolbar ul {
			list-style: none;
			padding: 0;
			margin: 0;
		}

		#toolbar ul li {
			float: left;
			padding-right: 1em;
			padding-bottom: 0.5em;
		}

		#toolbar ul li a, #time a {
			font-weight: bold;
			font-size: smaller;
			vertical-align: middle;
			color: black;
			text-decoration: none;
		}

		#toolbar ul li a:hover, #time a:hover {
		
		}

		#toolbar ul li * {
		
		}

		/* The map and the location bar */
		#map {
			clear: both;
			position: relative;
			width: ${request.width?c}px;
			height: ${request.height?c}px;
			border: 1px solid black;
		}

		#wrapper {
			width: ${request.width?c}px;
		}

		#location {
			float: right;
		}

		#options {
			position: absolute;
			left: 13px;
			top: 7px;
			z-index: 3000;
		}

		/* Styles used by the default GetFeatureInfo output, added to make IE happy */
		table.featureInfo, table.featureInfo td, table.featureInfo th {
			border: 1px solid #ddd;
			border-collapse: collapse;
			margin: 0;
			padding: 0;
			font-size: 90%;
			padding: .2em .1em;
		}

		table.featureInfo th {
			padding: .2em .2em;
			text-transform: uppercase;
			font-weight: bold;
			background: #eee;
		}

		table.featureInfo td {
			background: #fff;
		}

		table.featureInfo tr.odd td {
			background: #eee;
		}

		table.featureInfo caption {
			text-align: left;
			font-size: 100%;
			font-weight: bold;
			text-transform: uppercase;
			padding: .2em .2em;
		}

		#timeValues {
			width: 200px;
			height: 12em;
		}
		
		#timeValuesSelector {
			width: 240px;
			height: 12em;
		}

		#timeDimension {
			padding: 5px;
			width: 870px;
		}

		#timeRequest {
			padding: 10px;
			width: 510px;
			float: left;
		}	
		
		#timeDomain {
			padding: 10px;
			width: 300px;
			float: left;
		}

		#timeDomain label{
			width: 90px;
			float: left;
			margin-right: 0.5em;
			display: block;
		}

		#timeDomain p {
			line-height: 130%;
			margin-top: 0.8em;
		}

		#timeDomain input{
			width:180px;
		}

		#timeValuesWrapper{
			margin-top: 10px;
			width: 180px;
		}

		#removeTime input{
			margin:4px;
		}

		#timeSelection {
			float:left;
			padding-left: 10px;
		}

		#timeInput {
			float:left;			
		}

		.timeSelect {
			margin-bottom:30px;
		}

		/* css for timepicker */
		.ui-timepicker-div .ui-widget-header{ margin-bottom: 8px; }
		.ui-timepicker-div dl{ text-align: left; }
		.ui-timepicker-div dl dt{ height: 25px; }
		.ui-timepicker-div dl dd{ margin: -25px 0 10px 65px; }
		.ui-timepicker-div td { font-size: 90%; }

		#ui-datepicker-div{	z-index: 9999 !important; }
		
	</style>
	<!-- Import OpenLayers, reduced, wms read only version -->
	<script src="${baseUrl}/openlayers/OpenLayers.js" type="text/javascript"></script>
	<script src="${baseUrl}/js/jquery/jquery-1.5.min.js" type="text/javascript"></script>
	<script src="${baseUrl}/js/jquery/ui/js/jquery-ui-1.8.9.custom.min.js" type="text/javascript"></script>
	<script src="${baseUrl}/js/jquery/jquery-ui-timepicker-addon.js" type="text/javascript"></script>
	<link rel="stylesheet" type="text/css" href="${baseUrl}/js/jquery/ui/css/smoothness/jquery-ui-1.8.9.custom.css"/>

	<script defer="defer" type="text/javascript">
    var map;
    var untiled;
    var tiled;
    var pureCoverage = ${pureCoverage?string};
    var timePresentationMode = '${timePresentationMode?string}';
    var timeVal = 'current';
    // pink tile avoidance
    OpenLayers.IMAGE_RELOAD_ATTEMPTS = 5;
    // make OL compute scale according to WMS spec
    OpenLayers.DOTS_PER_INCH = 25.4 / 0.28;

    function init(){
    // if this is just a coverage or a group of them, disable a few items,
    // and default to jpeg format
    format = 'image/png';
    if(pureCoverage) {
      //document.getElementById('filterType').disabled = true;
      //document.getElementById('filter').disabled = true;
      //document.getElementById('updateFilterButton').disabled = true;
      //document.getElementById('resetFilterButton').disabled = true;
      document.getElementById('antialiasSelector').disabled = true;
      document.getElementById('jpeg').selected = true;
      format = "image/jpeg";
    }

    var bounds = new OpenLayers.Bounds(
      ${request.bbox.minX?c}, ${request.bbox.minY?c},
      ${request.bbox.maxX?c}, ${request.bbox.maxY?c}
    );
    var options = {
      controls: [],
      maxExtent: bounds,
      maxResolution: ${maxResolution?c},
      projection: "${request.SRS?js_string}",
      units: '${units?js_string}'
    };
    map = new OpenLayers.Map('map', options);

    // setup tiled layer
    tiled = new OpenLayers.Layer.WMS(
      "${layerName} - Tiled", "${baseUrl}/${servicePath}",
      {
      <#list parameters as param>
      ${param.name}: '${param.value?js_string}',
      </#list>
      format: format,
      tiled: !pureCoverage,
      tilesOrigin : map.maxExtent.left + ',' + map.maxExtent.bottom
      },
      {
      buffer: 0,
      displayOutsideMaxExtent: true
      }
    );

    // setup single tiled layer
    untiled = new OpenLayers.Layer.WMS(
      "${layerName} - Untiled", "${baseUrl}/${servicePath}",
      {
      <#list parameters as param>
      ${param.name}: '${param.value?js_string}',
      </#list>
      format: format
      },
      {singleTile: true, ratio: 1}
    );

    map.addLayers([untiled, tiled]);

    // build up all controls
    map.addControl(new OpenLayers.Control.PanZoomBar({
      position: new OpenLayers.Pixel(2, 15)
    }));
    map.addControl(new OpenLayers.Control.Navigation());
    map.addControl(new OpenLayers.Control.Scale($('scale')));
    map.addControl(new OpenLayers.Control.MousePosition({element: $('location')}));
    map.zoomToExtent(bounds);

    // wire up the option button
    var options = document.getElementById("options");
    options.onclick = toggleControlPanel;

    // support GetFeatureInfo
	function getFeatureInfo (e) {
		document.getElementById('nodelist').innerHTML = "Loading... please wait...";
		var params = {
			REQUEST: "GetFeatureInfo",
			EXCEPTIONS: "application/vnd.ogc.se_xml",
			BBOX: map.getExtent().toBBOX(),
			SERVICE: "WMS",
			VERSION: "1.1.1",
			X: e.xy.x,
			Y: e.xy.y,
			INFO_FORMAT: 'text/html',
			QUERY_LAYERS: map.layers[0].params.LAYERS,
			FEATURE_COUNT: 50,
			<#assign skipped=["request","bbox","width","height","format"]>
			<#list parameters as param>
				<#if !(skipped?seq_contains(param.name?lower_case))>
				${param.name?capitalize}: '${param.value?js_string}',
				</#if>
			</#list>
			WIDTH: map.size.w,
			HEIGHT: map.size.h,
			format: format,
			srs: map.layers[0].params.SRS,
			time: timeVal};
			OpenLayers.loadURL("${baseUrl}/${servicePath}", params, this, setHTML, setHTML);
			OpenLayers.Event.stop(e);
	}
	
    map.events.register('click', map, getFeatureInfo);
    }

    // sets the HTML provided into the nodelist element
    function setHTML(response){
		document.getElementById('nodelist').innerHTML = response.responseText;
    };

    // shows/hide the control panel
    function toggleControlPanel(event){
		var toolbar = document.getElementById("toolbar");
		if (toolbar.style.display == "none") {
			toolbar.style.display = "block";
		}
		else {
			toolbar.style.display = "none";
		}
		event.stopPropagation();
		map.updateSize()
    }

    // Tiling mode, can be 'tiled' or 'untiled'
    function setTileMode(tilingMode){
		if (tilingMode == 'tiled') {
			untiled.setVisibility(false);
			tiled.setVisibility(true);
			map.setBaseLayer(tiled);
		}
		else {
			untiled.setVisibility(true);
			tiled.setVisibility(false);
			map.setBaseLayer(untiled);
		}
    }

    // Transition effect, can be null or 'resize'
    function setTransitionMode(transitionEffect){
		if (transitionEffect === 'resize') {
			tiled.transitionEffect = transitionEffect;
			untiled.transitionEffect = transitionEffect;
		}
		else {
			tiled.transitionEffect = null;
			untiled.transitionEffect = null;
		}
    }

    // changes the current tile format
    function setImageFormat(mime){
		// we may be switching format on setup
		if(tiled == null)
			return;

		tiled.mergeNewParams({
			format: mime
		});
		untiled.mergeNewParams({
			format: mime
		});
		/*
		var paletteSelector = document.getElementById('paletteSelector')
		if (mime == 'image/jpeg') {
		  paletteSelector.selectedIndex = 0;
		  setPalette('');
		  paletteSelector.disabled = true;
		}
		else {
		  paletteSelector.disabled = false;
		}
		*/
    }

    // sets the chosen style
    function setStyle(style){
    // we may be switching style on setup
    if(tiled == null)
		return;

    tiled.mergeNewParams({
		styles: style
    });
    untiled.mergeNewParams({
		styles: style
    });
    }

    function setAntialiasMode(mode){
    tiled.mergeNewParams({
		format_options: 'antialias:' + mode
    });
    untiled.mergeNewParams({
		format_options: 'antialias:' + mode
    });
    }

    function setPalette(mode){
		if (mode == '') {
			tiled.mergeNewParams({
				palette: null
			});
			untiled.mergeNewParams({
				palette: null
			});
		}
		else {
			tiled.mergeNewParams({
				palette: mode
			});
			untiled.mergeNewParams({
				palette: mode
			});
		}
    }

    function setWidth(size){
		var mapDiv = document.getElementById('map');
		var wrapper = document.getElementById('wrapper');

		if (size == "auto") {
			// reset back to the default value
			mapDiv.style.width = null;
			wrapper.style.width = null;
		}
		else {
			mapDiv.style.width = size + "px";
			wrapper.style.width = size + "px";
		}
		// notify OL that we changed the size of the map div
		map.updateSize();
    }

    function setHeight(size){
		var mapDiv = document.getElementById('map');

		if (size == "auto") {
			// reset back to the default value
			mapDiv.style.height = null;
		}
		else {
			mapDiv.style.height = size + "px";
		}
		// notify OL that we changed the size of the map div
		map.updateSize();
    }

    function updateFilter(){
		if(pureCoverage)
			return;

		var filterType = document.getElementById('filterType').value;
		var filter = document.getElementById('filter').value;

		// by default, reset all filters
		var filterParams = {
			filter: null,
			cql_filter: null,
			featureId: null
		};

		if (OpenLayers.String.trim(filter) != "") {
		if (filterType == "cql")
			filterParams["cql_filter"] = filter;
		if (filterType == "ogc")
			filterParams["filter"] = filter;
		if (filterType == "fid")
			filterParams["featureId"] = filter;
		}
		// merge the new filter definitions
		mergeNewParams(filterParams);
    }

	function resetFilter() {
		if(pureCoverage)
			return;

		document.getElementById('filter').value = "";
		updateFilter();
	}

	function mergeNewParams(params){
		tiled.mergeNewParams(params);
		untiled.mergeNewParams(params);
	}
	
	// filtering by time
    function updateTimeParameter(){
	
		var times = [];
		var timeEl = document.getElementById('timeValuesSelector');

		for (var i=0; i<timeEl.options.length; i++) {
			times.push(timeEl.options[i].value);
		}
		
		timeVal = "";	
		timeVal = times.join();

		tiled.mergeNewParams({
			time: timeVal
		});
		untiled.mergeNewParams({
			time: timeVal
		});
	}

	function removeTime(selection){
		var timeEl = document.getElementById('timeValuesSelector');		
		for (var i=timeEl.options.length-1;i>=0; i--) {
			if(!selection || (selection && timeEl.options[i].selected)){
				timeEl.remove(i);
			}
		}
		
		if(!selection){
			tiled.mergeNewParams({
				time: ''
			});
			untiled.mergeNewParams({
				time: ''
			});
		}
    }

	function transferTimes(){
		var timeEl = document.getElementById('timeValues');		
		for (var i=timeEl.options.length-1;i>=0; i--) {
			if (timeEl.options[i].selected) {
				var time = timeEl.options[i].value;
				if(!timeExists(time)){
					$('#timeValuesSelector').
						append($("<option></option>").
						attr("value",time).
						text(time));
				}
			}
		}
	}
	
    function addTime(){
		var timeEl = document.getElementById('singleTime').value;		
		if(timeEl && !timeExists(timeEl)){
			$('#timeValuesSelector').
				append($("<option></option>").
				attr("value",timeEl).
				text(timeEl));
		}
    }
	
	function addTimeInterval(){	
		var timeStart = document.getElementById('intervalStartTime').value;		
		var timeEnd = document.getElementById('intervalEndTime').value;		
		var interval = timeStart+'/'+timeEnd;
		if(timeStart && timeEnd && !timeExists(interval)){			
			$('#timeValuesSelector').
			  append($("<option></option>").
			  attr("value",interval).
			  text(interval));
		}
	}
	
	var timeExists = function(val){
		var exists = $("#timeValuesSelector option[value='"+val+"']");
		if (exists && exists.length > 0){
			return true;
		}		
		return false;
	}
	
	$(document).ready(function() {
		$('#singleTime,#intervalStartTime,#intervalEndTime').datetimepicker({
			showSecond: true,
			separator: '',
			timeFormat: 'h:mm:ssZ',
			dateFormat: 'yy-mm-ddT'
		});

		if(timePresentationMode === 'interval'){
			$('#timeValuesWrapper').css('display', 'none');
		} else {
			$('#intResolutionWrapper').css('display', 'none');
		}
		
		var h1 = $('#timeDomain').css('height');
		var h2 = $('#timeRequest').css('height');
		if(h1 >= h2){
			$('#timeRequest').css('height', h1);
		} else {
			$('#timeDomain').css('height', h2);
		}
		
	});
  </script>
  </head>
  <body onload="init()">
  <div id="toolbar" style="display: none;">
    <ul>
    <li>
      <a>Tiling:</a>
      <select id="tilingModeSelector" onchange="setTileMode(value)">
      <option value="untiled">Single tile</option>
      <option value="tiled">Tiled</option>
      </select>
    </li>
    <li>
      <a>Transition effect:</a>
      <select id="transitionEffectSelector" onchange="setTransitionMode(value)">
      <option value="">None</option>
      <option value="resize">Resize</option>
      </select>
    </li>
    <li>
      <a>Antialias:</a>
      <select id="antialiasSelector" onchange="setAntialiasMode(value)">
      <option value="full">Full</option>
      <option value="text">Text only</option>
      <option value="none">Disabled</option>
      </select>
    </li>
    <li>
      <a>Format:</a>
      <select id="imageFormatSelector" onchange="setImageFormat(value)">
      <option value="image/png">PNG 24bit</option>
      <option value="image/png8">PNG 8bit</option>
      <option value="image/gif">GIF</option>
      <option id="jpeg" value="image/jpeg">JPEG</option>
      </select>
    </li>
    <li>
      <a>Styles:</a>
      <select id="imageFormatSelector" onchange="setStyle(value)">
      <option value="">Default</option>
      <#list styles as style>
        <option value="${style}">${style}</option>
      </#list>
      </select>
    </li>
    <!-- Commented out for the moment, some code needs to be extended in
       order to list the available palettes
    <li>
      <a>Palette:</a>
      <select id="paletteSelector" onchange="setPalette(value)">
      <option value="">None</option>
      <option value="safe">Web safe</option>
      </select>
    </li>
    -->
    <li>
      <a>Width/Height:</a>
      <select id="widthSelector" onchange="setWidth(value)">
      <!--
      These values come from a statistics of the viewable area given a certain screen area
      (but have been adapted a litte, simplified numbers, added some resolutions for wide screen)
      You can find them here: http://www.evolt.org/article/Real_World_Browser_Size_Stats_Part_II/20/2297/
      --><option value="auto">Auto</option>
      <option value="600">600</option>
      <option value="750">750</option>
      <option value="950">950</option>
      <option value="1000">1000</option>
      <option value="1200">1200</option>
      <option value="1400">1400</option>
      <option value="1600">1600</option>
      <option value="1900">1900</option>
      </select>
      <select id="heigthSelector" onchange="setHeight(value)">
      <option value="auto">Auto</option>
      <option value="300">300</option>
      <option value="400">400</option>
      <option value="500">500</option>
      <option value="600">600</option>
      <option value="700">700</option>
      <option value="800">800</option>
      <option value="900">900</option>
      <option value="1000">1000</option>
      </select>
    </li>
    <li>
      <a>Filter:</a>
      <select id="filterType">
      <option value="cql">CQL</option>
      <option value="ogc">OGC</option>
      <option value="fid">FeatureID</option>
      </select>
      <input type="text" size="80" id="filter"/>
      <img id="updateFilterButton" src="${baseUrl}/openlayers/img/east-mini.png" onClick="updateFilter()" title="Apply filter"/>
      <img id="resetFilterButton" src="${baseUrl}/openlayers/img/cancel.png" onClick="resetFilter()" title="Reset filter"/>
    </li>
    </ul>
	<br />
  </div>

  <div id="timePanel">
    <fieldset id="timeDimension">
    <legend>Time Dimension</legend>
    <fieldset id="timeDomain">
      <legend>Time Domain</legend>
		  <p>
			<label for="intMinimum">Minimum</label>
			<input type="text" name="intMinimum" readonly="readonly" id="intMinimum" value="${timeIntervalStart?string}" />
		  </p>
		  <p>
			<label for="intMinimum">Maximum</label>
			<input type="text" name="intMaximum"  readonly="readonly" id="intMaximum" value="${timeIntervalEnd?string}" />
		  </p>
		  <p id="intResolutionWrapper">
			<label for="intResolution">Resolution</label>
			<input type="text" name="intResolution"  readonly="readonly" id="intResolution" value="${timeResolution?string}" />
		  </p>
		
		  <div id="timeValuesWrapper">
			  <select id='timeValues' multiple="multiple" >
				<#list timeDimension as time>
				  <option value="${time}">${time}</option>
				</#list>
			  </select>
			  <input type='button' value='Add to selection' onclick='transferTimes()' />
		  </div>

    </fieldset>
    <fieldset id="timeRequest">
      <legend>Time Request</legend>
			<div id="timeInput">
				<div class="timeSelect">
					<input type="text" name="singleTime" id="singleTime" value="" />
					<input type='button' value='Add Time' onclick='addTime()' />				
				</div>
				<div class="timeSelect">
					<input type="text" name="intervalStartTime" id="intervalStartTime" value="" /><br />
					<input type="text" name="intervalEndTime" id="intervalEndTime" value="" />
					<input type='button' value='Add Interval' onclick='addTimeInterval()' />
				</div>
			</div>
			<div id="timeSelection">
				<select id='timeValuesSelector' onchange='setTimeDimension()' multiple="multiple">
				</select>
				<div id="removeTime">
					<input type='button' value='Remove Selected' onclick='removeTime(true)' />
					<br />
					<input type='button' value='Remove All' onclick='removeTime()' />
					<br />
					<input type='button' value='Send Request' onclick='updateTimeParameter()' />
				</div>				
			</div>
    </fieldset>
    </fieldset>
  </div>

  <div id="map">
    <img id="options" title="Toggle options toolbar" src="${baseUrl}/options.png"/>
  </div>
  <div id="wrapper">
    <div id="location">location</div>
    <div id="scale">
    </div>
  </div>
  <div id="nodelist">
    <em>Click on the map to get feature info</em>
  </div>
  </body>
</html>
