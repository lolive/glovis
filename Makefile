.SUFFIXES: .class .java

SRC	= imgViewer.java \
	AddressSearchMapLayer.java \
	AddressTable.java \
	AddressTablePanel.java \
		AntarcticaEtmDataset.java \
		ArrowPane.java \
		AsterSensor.java \
		AsterVNIRSensor.java \
		AsterTIRSensor.java \
		AsterVNIRDataPoolSensor.java \
		AsterTIRDataPoolSensor.java \
		AttributeTable.java \
		CancelLoad.java \
		CheckBoxPanel.java \
		CloudCoverChoice.java \
		CloudCoverLimit.java \
		CombinedSceneList.java \
		ConfirmScenesDialog.java \
		CreateProjection.java \
		DateCache.java \
		DBFReader.java \
		DoublePoint.java \
		DownloadData.java \
		EarthExplorer.java \
		EO1AliSensor.java \
		EO1HypSensor.java \
		EO1Sensor.java \
		FileDownloader.java \
		FileMenu.java \
		FocusTextField.java \
		GridCellEntry.java \
		GridMapLayer.java \
		GeographicLocatorMap.java \
		GeographicLocatorMapConfig.java \
	GeographicProjection.java \
		Gls1975Mss1_3Dataset.java \
		Gls1975Mss4_5Dataset.java \
		Gls1990Dataset.java \
		Gls2000Dataset.java \
		Gls2005Dataset.java \
		Gls2005EO1Dataset.java \
		Gls2010Dataset.java \
		Gls2010EO1Dataset.java \
		SystematicL1GDataset.java \
		HelpMenu.java \
		HideSceneDialog.java \
	NalcDataset.java \
	NappDataset.java \
	NappFlightlineModel.java \
	NhapDataset.java \
	NhapNavModel.java \
		OrthoTMDataset.java \
		OrthoETMDataset.java \
		OrthoPanSharpETMDataset.java \
		Ortho1_3MssDataset.java \
		Ortho4_5MssDataset.java \
		ImageLoader.java \
		ImagePane.java \
		LamAzProjection.java \
		LandsatSensor.java \
		Landsat1_3MssSensor.java \
		Landsat4_5MssSensor.java \
		LandsatTMSensor.java \
		LandsatETMSensor.java \
		LandsatETMSlcOffSensor.java \
		Landsat8OLISensor.java \
		LandsatCombined.java \
		LandsatSceneFilter.java \
		LatLong.java \
		LatLongEntry.java \
		LatLongToModisTile.java \
		LineGraph.java \
		LineMapLayer.java \
		ListEntryBuilder.java \
		LocatorMap.java \
		LocatorMapConfig.java \
		LocatorMapImpl.java \
		ll2PathRow.java \
		LogoImage.java \
		MapLayer.java \
		MapLayerFeatureInfo.java \
		MapLayerFileCache.java \
		MapLayerLoadingCallback.java \
		MapLayerMenu.java \
		MapLayers.java \
		MapLayersConfig.java \
		Metadata.java \
		MinBox.java \
		ModisSceneFilter.java \
		ModisSensor.java \
		ModisTile.java \
		ModisTileMapLayer.java \
		ModisTileModel.java \
		MonthChoice.java \
		MosaicCoords.java \
		MosaicData.java \
		Mrlc2001TCDataset.java \
		Mrlc2001RADataset.java \
		MrlcDataset.java \
		NavigateDate.java \
	NDVIGraphDialog.java\
	NDVILineGraph.java\
		NorthArrowMapLayer.java \
		PolygonIntersectTester.java \
		PointOfInterestDialog.java \
		PointOfInterestMapLayer.java \
	PolarNavModel.java \
	PolarStereographicLocatorMap.java \
	PolarStereographicLocatorMapConfig.java \
	PolarStereographicProjection.java \
		ProgressIndicator.java \
		ProjectionTransformation.java \
		ResolutionMenu.java \
		SaveOrLoadSceneLists.java \
		SearchForSceneDialog.java \
	SearchForAddressDialog.java \
		SceneInfo.java \
		SceneFilter.java \
		SceneList.java \
		SceneListDialog.java \
		SceneListList.java \
		SceneListMenu.java \
		SceneListPanel.java \
		SceneMenu.java \
	SceneOverlayMapLayer.java \
		SearchLimitDialog.java \
		Sensor.java \
		SensorMenu.java \
		ShapeFileAttributesDialog.java \
		ShapeFileAttributesPanel.java \
		ShapeFileMapLayer.java \
		ShapeFileReader.java \
		SinusoidalLocatorMap.java \
		SinusoidalLocatorMapConfig.java \
		SinusoidalProjection.java \
		StatusBar.java \
		SwathSceneFilter.java \
	TerraLookAsterVNIRSensor.java \
		TiledMapLayer.java \
		TOC.java \
		ToolsMenu.java \
	TriDecEtmMosaicDataset.java \
	TriDecEtmMosaicModel.java \
	TriDecTmMosaicDataset.java \
	TriDecTmMosaicModel.java \
	USLocatorMapConfig.java \
	UtmZoneMapLayer.java \
		WorkMonitor.java \
		WorldCitiesMapLayer.java \
		WrsGridCenterMapLayer.java \
		WRS1Model.java \
		WRS2AscendingModel.java \
		WRS2Model.java \
		ZOrderList.java

CGI =   annotateSlcOffBrowse.cgi \
	getBrowse.cgi \
	getBrowseImage.cgi \
	getOverviewMap.cgi \
	geocodeGoogle.cgi \
	showbrowse.cgi \
	showmetadata.cgi \
	showAsterBrowse.cgi \
	showAsterMetadata.cgi \
	showEO1Browse.cgi \
	showEO1Metadata.cgi \
	showLandsatL1Browse.cgi \
	showLandsatL1Metadata.cgi \
	showModisBrowse.cgi \
	showModisMetadata.cgi\
	showMosaicBrowse.cgi\
	showMosaicMetadata.cgi\
	showMrlcBrowse.cgi \
	showMrlcMetadata.cgi \
	showNalcBrowse.cgi \
	showNalcMetadata.cgi \
	showNappBrowse.cgi \
	showNappMetadata.cgi \
	showNhapBrowse.cgi \
	showNhapMetadata.cgi \
	showOrthoBrowse.cgi \
	showOrthoMetadata.cgi \
	AsterFGDC.cgi\
	ModisFGDC.cgi\
	MrlcFGDC.cgi\
	OrthoFGDC.cgi\
	searchForScene.cgi

HTML =  \
	glossary_aster.html \
	glossary_eo1.html \
	glossary_gls.html \
	glossary_mosaic.html \
	glossary_nalc.html\
	glossary_napp.html\
	glossary_nhap.html\
	glossary_ortho_tm.html\
	glossary_ortho_pan_etm.html\
	glossary_ortho_mss1_3.html\
	glossary_ortho_mss4_5.html\
	glossary_modis.html\
	glossary_mrlc_2001_tc.html\
	glossary_mrlc_2001_ra.html\
	landsat_dictionary.html \
	acqSchedule.html \
	LandcoverHelp.html

CLASSES	= $(SRC:.java=.class)

GIFS	=	graphics/ctxmarker.gif \
		graphics/glovis.gif \
		graphics/downbutton.gif \
		graphics/leftbutton.gif \
		graphics/rightbutton.gif \
		graphics/upbutton.gif \
		graphics/downbutton1.gif \
		graphics/downbutton2.gif \
		graphics/leftbutton1.gif \
		graphics/leftbutton2.gif \
		graphics/rightbutton1.gif \
		graphics/rightbutton2.gif \
		graphics/upbutton1.gif \
		graphics/upbutton2.gif \
		graphics/WorldBoundariesBlack.gif \
	graphics/USBoundariesBlack.gif \
		graphics/NASA_small.gif \
		graphics/USGS_logo.gif \
		graphics/pushPin.gif \
		graphics/pushPinYellow.gif \
		graphics/downloadIcon.gif \
		graphics/download_extra_small.gif


JPGS = graphics/World5Minute.jpg graphics/modistiles.jpg graphics/USMap.jpg \
       graphics/antarctica.jpg

PROPERTIES = GloVis.Properties

INST_DIR = $(GV_HTML)/ImgViewer
GRAPHICS_DIR = $(INST_DIR)/graphics
SEC_DIR = $(INST_DIR)/.security

default : java2ImgViewer.jar sign

java2ImgViewer.jar: geocodeGoogle $(CLASSES)
	jar cmf java_manifest java2ImgViewer.jar *.class

geocodeGoogle:
	@if [ -e $(SEC_DIR)/geocodeGoogle.pm ]; then \
	touch $(INST_DIR)/searchenabled ;  \
	else /bin/rm -f $(INST_DIR)/searchenabled ; fi

# Note: use -Xlint:unchecked to find unsafe features
.java.class:
	javac -Xlint:deprecation $<

sign:
	@if [ -n "$(GV_JAVASEC_KEYSTORE)" -a -n "$(GV_JAVASEC_PASSWORD)" -a \
	      -n "$(GV_JAVASEC_ALIAS)" -a -e $(GV_JAVASEC_KEYSTORE) ] ; then \
	jarsigner -keystore $(GV_JAVASEC_KEYSTORE) \
    -storepass $(GV_JAVASEC_PASSWORD) java2ImgViewer.jar $(GV_JAVASEC_ALIAS); fi

clean:
	rm -f *.class java2ImgViewer.jar

all: clean install

install: default
	@if [ -z $(GV_HTML) ] ; then \
	echo "Error: GV_HTML not defined" ; exit 1;  fi
	@if [ ! -d $(INST_DIR) ] ; then mkdir -p $(INST_DIR) ; fi 
	@if [ ! -d $(GRAPHICS_DIR) ] ; then mkdir -p $(GRAPHICS_DIR) ; fi 
	@if [ ! -L $(INST_DIR)/l8oli -a ! -d $(INST_DIR)/l8oli ] ; then \
	ln -sf $(GV_INVENTORY)/l8oli $(INST_DIR)/l8oli ; fi
	@if [ ! -L $(INST_DIR)/l7 -a ! -d $(INST_DIR)/l7 ] ; then \
	ln -sf $(GV_INVENTORY)/l7 $(INST_DIR)/l7 ; fi
	@if [ ! -L $(INST_DIR)/l7slc_off -a ! -d $(INST_DIR)/l7slc_off ] ; then \
	ln -sf $(GV_INVENTORY)/l7slc_off $(INST_DIR)/l7slc_off ; fi
	@if [ ! -L $(INST_DIR)/l5 -a ! -d $(INST_DIR)/l5 ] ; then \
	ln -sf $(GV_INVENTORY)/l5 $(INST_DIR)/l5 ; fi
	@if [ ! -L $(INST_DIR)/l4_5mss -a ! -d $(INST_DIR)/l4_5mss ] ; then \
	ln -sf $(GV_INVENTORY)/l4_5mss $(INST_DIR)/l4_5mss ; fi
	@if [ ! -L $(INST_DIR)/l1_3mss -a ! -d $(INST_DIR)/l1_3mss ] ; then \
	ln -sf $(GV_INVENTORY)/l1_3mss $(INST_DIR)/l1_3mss ; fi
	@if [ ! -L $(INST_DIR)/ortho -a ! -d $(INST_DIR)/ortho ] ; then \
	ln -sf $(GV_INVENTORY)/ortho $(INST_DIR)/ortho ; fi
	@if [ ! -L $(INST_DIR)/mrlc_2001_tc -a ! -d $(INST_DIR)/mrlc_2001_tc ] ; then \
	ln -sf $(GV_INVENTORY)/mrlc_2001_tc $(INST_DIR)/mrlc_2001_tc ; fi
	@if [ ! -L $(INST_DIR)/mrlc_2001_ra -a ! -d $(INST_DIR)/mrlc_2001_ra ] ; then \
	ln -sf $(GV_INVENTORY)/mrlc_2001_ra $(INST_DIR)/mrlc_2001_ra ; fi
	@if [ ! -L $(INST_DIR)/aster -a ! -d $(INST_DIR)/aster ] ; then \
	ln -sf $(GV_INVENTORY)/aster $(INST_DIR)/aster ; fi
	@if [ ! -L $(INST_DIR)/aster_datapool -a ! -d $(INST_DIR)/aster_datapool ] ; then \
	ln -sf $(GV_INVENTORY)/aster_datapool $(INST_DIR)/aster_datapool ; fi
	@if [ ! -L $(INST_DIR)/modis -a ! -d $(INST_DIR)/modis ] ; then \
	ln -sf $(GV_INVENTORY)/modis $(INST_DIR)/modis ; fi
	@if [ ! -L $(INST_DIR)/eo1 -a ! -d $(INST_DIR)/eo1 ] ; then \
	ln -sf $(GV_INVENTORY)/eo1 $(INST_DIR)/eo1 ; fi
	@if [ ! -L $(INST_DIR)/nalc -a ! -d $(INST_DIR)/nalc ] ; then \
	ln -sf $(GV_INVENTORY)/nalc $(INST_DIR)/nalc ; fi
	@if [ ! -L $(INST_DIR)/napp -a ! -d $(INST_DIR)/napp ] ; then \
	ln -sf $(GV_INVENTORY)/napp $(INST_DIR)/napp ; fi
	@if [ ! -L $(INST_DIR)/nhap -a ! -d $(INST_DIR)/nhap ] ; then \
	ln -sf $(GV_INVENTORY)/nhap $(INST_DIR)/nhap ; fi
	@if [ ! -L $(INST_DIR)/lsat_sys -a ! -d $(INST_DIR)/lsat_sys ] ; then \
	ln -sf $(GV_INVENTORY)/lsat_sys $(INST_DIR)/lsat_sys ; fi
	@if [ ! -L $(INST_DIR)/gls -a ! -d $(INST_DIR)/gls ] ; then \
	ln -sf $(GV_INVENTORY)/gls $(INST_DIR)/gls ; fi
	@if [ ! -L $(INST_DIR)/linework -a ! -d $(INST_DIR)/linework ] ; then \
	ln -sf $(GV_INVENTORY)/linework_version7 $(INST_DIR)/linework ; fi
	@if [ ! -L $(INST_DIR)/NDVI -a ! -d $(INST_DIR)/NDVI -a -d $(GV_INVENTORY)/NDVI ] ; then \
	    ln -sf $(GV_INVENTORY)/NDVI $(INST_DIR)/NDVI ; fi
	@if [ -L $(INST_DIR)/NDVI -o -d $(INST_DIR)/NDVI ] ; then \
	    cat >| $(INST_DIR)/NDVI_EXISTS.TXT << /dev/null ; fi
	if [ ! -d $(SEC_DIR) ] ; then mkdir -p $(SEC_DIR) ; fi
	cp java2ImgViewer.jar $(INST_DIR)
	cp $(GIFS) $(GRAPHICS_DIR)
	cp $(JPGS) $(GRAPHICS_DIR)
	cp $(HTML) $(INST_DIR)
	cp $(CGI) $(INST_DIR)
	cp $(PROPERTIES) $(INST_DIR)
	cp nobody_htaccess $(SEC_DIR)/.htaccess

