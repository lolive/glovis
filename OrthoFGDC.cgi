#!/usr/bin/perl -wT
# This cgi script builds a web page to show the metadata for the list of
# scene id's passed in

# use strict mode
use strict;

# use the CGI module
use CGI qw(:standard escapeHTML);

# get the scene id & order id passed in
my $taintedsceneid = param('scene_id');
my $taintedpath = param('path');
my $taintedrow = param('row');
my $tainted_acqDate = param('acq_date');
my $tainted_NBound = param('NBound');
my $tainted_SBound = param('SBound');
my $tainted_WBound = param('WBound');
my $tainted_EBound = param('EBound');

# unencode the scene id
#$taintedsceneid =~ s/%(..)/pack("c",hex($1))/ge;

# strip the version number from the scene id and make sure the rest of the
# scene id is untainted
my $platform;
my $sceneid;
if ($taintedsceneid =~ /^(ET[\d\w]{18})$/)
{
    $sceneid = $1;
    $platform = "tm";
}
elsif ($taintedsceneid =~ /^(EL[\d\w]{19})$/)
{
    $sceneid = $1;
    $platform = "etm";
}
elsif ($taintedsceneid =~ /^(EP[\d\w]{19})$/)
{
    $sceneid = $1;
    $platform = "etmpan";
}
elsif ($taintedsceneid =~ /^(EM[\d\w]{18})$/)
{
    $sceneid = $1;
    my $sat_num = substr($sceneid,10,1);
    if ($sat_num < 4)
    {
        $platform = "mss1_3";
    }
    else
    {
        $platform = "mss4_5";
    }
}
else
{
    handleerror("Scene ID $taintedsceneid is not legal"); # exits
}
my $path;
# untaint the path and row
if ($taintedpath =~/^(\d{1,3})$/)
{
    $path = $1;
    $path = sprintf("%03d", $path);
}
else
{
    handleerror("Path $taintedpath is not legal"); # exits
}
my $row;
if ($taintedrow =~/^(\d{1,3})$/)
{
    $row = $1;
    $row = sprintf("%03d", $row);
}
else
{
    handleerror("Row $taintedrow is not legal"); # exits
}

# untaint the aqcuisition date
unless($tainted_acqDate =~ m#^([\w.-]+)$#)
{
    handleerror("Acquisition Date $tainted_acqDate is not valid");
}
my $acqDate = $1;

if ($acqDate !~ m/^\d{2}-\d{2}-\d{4}$/)
{
    handleerror("Acquisistion Date $acqDate is not legal"); # exits
}

# untaint the north bounding latitude
unless($tainted_NBound =~ m#^([\w.-]+)$#)
{
    handleerror("North Bound $tainted_NBound is not valid");
}

my $NBound = $1;

if ($NBound !~ m/^\X{1,11}?$/)
{
    handleerror("NBound value $NBound is not legal"); # exits
}

# untaint the south bounding latitude
unless($tainted_SBound =~ m#^([\w.-]+)$#)
{
    handleerror("South Bound $tainted_SBound is not valid");
}

my $SBound = $1;

if ($SBound !~ m/^\X{1,11}?$/)
{
    handleerror("SBound value $SBound is not legal"); # exits
}

# untaint the west bounding longitude
unless($tainted_WBound =~ m#^([\w.-]+)$#)
{
    handleerror("West Bound $tainted_WBound is not valid");
}

my $WBound = $1;

if ($WBound !~ m/^\X{1,12}?$/)
{
    handleerror("WBound value $WBound is not legal"); # exits
}

# untaint the east bounding longitude
unless($tainted_EBound =~ m#^([\w.-]+)$#)
{
    handleerror("East Bound $tainted_EBound is not valid");
}

my $EBound = $1;

if ($EBound !~ m/^\X{1,12}?$/)
{
    p("EBound value $EBound is not legal"); # exits
}

# Using this information to be used for the different web page display depending
# on what sensor we are displaying.
my $sensor;
my $title;
my $abstract;
my $metadata_date;
if ($platform eq "tm")
{
  $sensor = "Landsat Orthorectified TM";
  $abstract = "Landsat Orthorectified TM: This data set contains thematic ".
  "mapper (TM) sensor data from the Landsat-4 and -5 satellites. The data ".
  "set uses a process called orthorectification to correct for positional".
  " accuracy and relief displacement." ;
  $metadata_date = "March 30, 2005";
}
elsif ($platform eq "etm")
{
    $sensor = "Landsat Orthorectified ETM+";
    $abstract = "Landsat Orthorectified ETM+ Product: This data uses a ".
    "process called orthorectification to correct for positional accuracy".
    " and relief displacement. Collection images were screened against the".
    " following criteria: acquisition date, cloud percentage, data quality".
    " parameters, and best available phenology.";
    $metadata_date ="March 30, 2005";
}
elsif ($platform eq "etmpan")
{
    $sensor = "Landsat Orthorectified Pansharpened ETM+";
    $abstract ="Landsat Orthorectified Pansharpened ETM+ Product: This data".
    " set contains enhanced thematic mapper (ETM+) sensor data from the ".
    "Landsat-7 satellite. The data set uses a process called ".
    "orthorectification to correct for positional accuracy and relief ".
    "displacement, and an additional process called pansharpening to ".
    "fuse together high-resolution panchromatic and lower-resolution ".
    "multispectral imagery in order to create a single high-resolution ".
    "color image.";
    $metadata_date ="March 30, 2005";
}
elsif (($platform eq "mss1_3") || ($platform eq "mss4_5"))
{
  $sensor = "Landsat Orthorectified MSS";
  $abstract = "Landsat Orthorectified MSS: This data set contains ".
  "multi-spectral scanner (MSS) sensor data from the Landsat-1 through -5 ".
  "satellites.  The data set uses a process called orthorectification to ".
  "correct for positional accuracy and relief displacement." ;
  $metadata_date = "March 30, 2005";
}
$title = "$sensor - Path: $path Row: $row for Scene $sceneid";

# javascript to set the window size.  Note that different browsers need hacks
# to get the sizing right.
my $JSCRIPT=<<END;
// Init function to set the size
function init()
{
    isNS4=(document.layers)?true:false;
    isIE4=(document.all && !document.getElementById)?true:false;
    isIE5=(document.all && document.getElementById)?true:false;
    isNS6=(!document.all && document.getElementById)?true:false;

    if (document.images)
    {
        winHeight = document.images[0].height + 170
        winWidth = document.images[0].width + 50
        // adjust the size if Net 4
        if (isNS4)
        {
            winHeight = winHeight - 150
            winWidth = winWidth - 30
        }
        // make sure we do not exceed screen size
        if (winHeight > screen.availHeight)
            winHeight = screen.availHeight
        if (winWidth > screen.availWidth)
            winWidth = screen.availWidth

        resizeTo(winWidth, winHeight)
    }
}
END
# build the web page
print header();
print start_html(-title=>"USGS Global Visualization Viewer - Scene $sceneid",
                 -script=>$JSCRIPT, -onLoad=>'init()');
buildPage();

# routine to generate an error page  
sub handleerror
{
    my @messages = @_; # for debugging - do not print details in prod
    print header();
    print start_html("USGS Global Visualization Viewer - FGDC Error");
    print p("Error generating FGDC page");
    #foreach (@messages)
    #{
    #    print p(@_);
    #}
    print end_html();
    exit;
}

sub buildPage
{
print<<END

<h1>$title</h1>
<p>
<h2>Metadata:</h2>
<ul>
<li><A HREF="#Identification_Information">Identification_Information</A></li>
<li><A HREF="#Metadata_Reference_Information">Metadata_Reference_Information
</A></li>
</ul>
<hr>
<dl>
<dt><a NAME="Identification_Information"></a>
<b>Identification_Information:</b></dt>  
<dd>
<dl>
<dt><b>Citation:</b></dt>  
<dd>
<dl>
<dt><b>Citation_Information:</b></dt> 
<dd>
<dl><br>
<dt><b>Originator:</b> USGS Earth Resources Observation &amp; Science
(EROS)</dt>
<dd>
<dt><b>Publication_Date:</b> $acqDate</dt>
<dt><b>Title:</b> $title</dt>
<dt><b>Edition:</b></dt>
<dt><b>Geospatial_Data_Presentation_Form:</b></dt>
<dt><b>Series_Information:</b></dt>
<dt><b>Series_Name:</b> Landsat</dt>
<dt><b>Issue_Identification:</b></dt>
<dt><b>Publication_Information:</b></dt>
<dt><b>Publication_Place:</b> US Geological Survey
</dt>
<dt><b>Publisher:</b> US Geological Survey</dt>
<dt><b>Online_Linkage:</b> (URL: http://eros.usgs.gov/)
</dt>
<dt><b>Other_Citation_Details:</b></dt>
</dd>
</dl>
</dd>
</dl>
</dd>
</dl>
<dl>
<dt><b>Description:</b></dt>
<dd>
<dl>
<dt><b>Abstract:</b> $abstract</dt>
<dt><b>Purpose:</b> These data sets were created as contributions to The 
National Map and with the intent of stimulating broader use of Landsat 
data by the land data user community.
</dt>
</dl>
</dd>
</dl>
<dl>
<dt><b>Time_Period_of_Content: </b></dt>
<dd>
<dl>
<dt><b>Time_Period_Information:</b></dt>
<dd>
<dl>
<dt><b>Single_Date/Time:</b></dt>
<dd>
<dl>
<dt><b>Calendar_Date:</b> $acqDate</dt>
</dl>
</dd>
<dt><b>Currentness_Reference:</b>Raw data time/date stamp;</dt>
</dl>
</dd>
</dl>
</dd>
</dl>
<dl>
<dt><b>Status:</b></dt>
<dd>
<dl>
<dt><b>Progress:</b> (Complete)</dt>
<dt><b>Maintenance_and_Update_Frequency:</b> (The frequency with which 
changes and additions are made to the data set after the initial data 
set is completed.)</dt>
</dl>
</dd>
</dl>
<dl>
<dt><b>Spatial_Domain:</b></dt>
<dd>
<dl>
<dt><b>Bounding_Coordinates:</b></dt>
<dd>
<dl>
<dt><b>West_Bounding_Coordinate:</b> $WBound</dt>
<dt><b>East_Bounding_Coordinate:</b> $EBound</dt>
<dt><b>North_Bounding_Coordinate:</b> $NBound</dt>
<dt><b>South_Bounding_Coordinate:</b> $SBound</dt>
</dl>
</dd>
</dl>
</dd>
</dl>
<dl>
<dt><b>Keywords:</b></dt>
<dd>
<dl>
<dt><b>Theme:</b></dt>
<dd>
<dl>
<dt><b>Theme_Keyword_Thesaurus:</b> none</dt>
<dt><b>Theme_Keyword:</b> Landsat</dt>
<dt><b>Theme_Keyword:</b> Orthorectified</dt>
<dt><b>Theme_Keyword:</b> Phenology</dt>
<dt><b>Theme_Keyword:</b> Greenness</dt>
<dt><b>Theme_Keyword:</b> Land Use</dt>
<dt><b>Theme_Keyword:</b> Land Cover</dt>
</dl>
</dd>
</dl>
<dl>
<dt><b>Place:</b></dt>
<dd>
<dl>
<dt><b>Place_Keyword_Thesaurus:</b> none</dt>
<dt><b>Place_Keyword:</b> Conus</dt>
<dt><b>Place_Keyword:</b> U.S.</dt>
<dt><b>Place_Keyword:</b> North America</dt>
</dl>
</dd>
</dl>
</dd>
</dl>
<dl>
<dt><b>Access_Constraints:</b> None</dt>
</dl>
<dl>
<dt><b>Use_Constraints:</b> None.  Acknowledgement of the U.S. 
Geological Survey would be appreciated in products derived from this data.</dt>
</dl>
<dl>
<dt><b>Point_of_Contact: </b></dt>
<dd>
<dl>
<dt><b>Contact_Information:</b></dt>
<dd>
<dl>
<dt><b>Contact_Organization_Primary:</b></dt>
<dd>
<dl>
<dt><b>Contact_Organization:</b> U.S. Geological Survey</dt>
<dt><b>Contact_Person:</b> Customer Service Representative</dt>
<dt><b>Contact_Address:</b></dt>
<dt><b>Address_Type:</b> mailing and physical address</dt>
<dt><b>Address:</b> USGS Earth Resources Observation &amp; Science (EROS)</dt>
<dt><b>Address:</b> 47914 252nd Street</dt>
<dt><b>City:</b> Sioux Falls</dt>
<dt><b>State_or_Province:</b> South Dakota (SD)</dt>
<dt><b>Postal_Code:</b> 57198-0001</dt>
<dt><b>Country:</b> United States of America (USA)</dt>
</dl>
</dd>
</dl>
<dl>
<dt><b>Contact_Voice_Telephone:</b> 605-594-6151 or 800-252-4547 
(tollfree)</dt>
<dt><b>Contact_Facsimile_Telephone:</b> 605-594-6589</dt>
<dt><b>Contact_Electronic_Mail_Address:</b> custserv\@usgs.gov</dt>
<dt><b>Hours_of_Service:</b> 0800 - 1600 CT, M - F (-6h CST/-5h 
CDT GMT)</dt>
</dl>
</dd>
</dl>
</dl>
<dl>
<dt><b>Data_Set_Credit:</b> U.S. Geological Survey</dt>
<dt><b>Security_Information:</b></dt>
<dd>
<dl>
<dt><b>Security_Classification_System:</b> None</dt>
<dt><b>Security_Classification:</b> Unclassified</dt>
<dt><b>Security_Handling_Description:</b> N/A</dt>
</dl>
</dd>
</dl>
</dl>
<hr>
<dl>
<dt><a NAME="Metadata_Reference_Information"></a>
<b>Metadata_Reference_Information:</b> </dt>
<dd>
<dl>
<dt><b>Metadata_Date:</b> $metadata_date</dt>
<dt><b>Metadata_Contact:</b></dt>
<dd>
<dl>
<dt><b>Contact_Information:</b> </dt>
<dd>
<dl>
<dt><b>Contact_Person_Primary:</b> </dt>
<dt><b>Contact_Organization:</b> U.S. Geological Survey</dt>
<dd>
<dl>
<dt><b>Contact_Person:</b> Customer Service Representative</dt>
<dt><b>Contact_Address:</b> </dt>
<dt><b>Address_Type:</b> mailing and physical address </dt>
<dt><b>Address:</b> USGS Earth Resources Observation &amp; Science (EROS)</dt>
<dt><b>Address:</b> 47914 252nd Street</dt>
<dt><b>City:</b> Sioux Falls</dt>
<dt><b>State_or_Province:</b> South Dakota (SD)</dt>
<dt><b>Postal_Code:</b> 57198-0001</dt>
<dt><b>Country:</b> United States of America (USA) </dt>
</dl>
</dd>
<dt><b>Contact_Voice_Telephone:</b> 605-594-6151 or 800-252-4547 
(toll free) </dt>
<dt><b>Contact_Facsimile_Telephone:</b> 605-594-6589 </dt>
<dt><b>Contact_Electronic_Mail_Address:</b> custserv\@usgs.gov</dt>
<dt><b>Hours_of_Service:</b> 0800 - 1600 CT, M - F (-6h CST/-5h 
CDT GMT)</dt> 
</dd>
</dl>
<dt><b>Metadata_Standard_Name:</b> FGDC Content Standards for 
					Digital Geospatial Metadata </dt>
<dt><b>Metadata_Standard_Version:</b> FGDC-STD-001-1998 </dt>
<dt><b>Metadata_Time_Convention:</b> (local time)</dt>
<dt><b>Metadata_Access_Constraints:</b> None</dt>
<dt><b>Metadata_Use_Constraints:</b> None</dt>
</dd>
</dl>
</dd>
</dl>
</dl>

END
}
print end_html();
exit;

