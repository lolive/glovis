#!/usr/bin/perl -wT
# This cgi script builds a web page to show the metadata for the list of
# scene id's passed in

# use strict mode
use strict;

# use the CGI module
use CGI qw(:standard escapeHTML);

# get the scene id & order id passed in
my $tainted_sceneid = param('scene_id');
my $tainted_dataset = param('dataset');
my $tainted_acqDate = param('acq_date');
my $tainted_LLLong = param('LLLong');
my $tainted_URLong = param('URLong');
my $tainted_ULLat = param('ULLat');
my $tainted_LRLat = param('LRLat');

# untaint the scene id
unless($tainted_sceneid =~ m#^(\w{19,21})$#)
{
    handleerror("Scene ID $tainted_sceneid is not valid");
}
my $sceneid = $1;

# make sure it is really a scene id
$_ = $sceneid;
if (!m/^\w{19,21}$/)
{
    handleerror("sceneid $sceneid is not legal");
}

# make sure it is a valid dataset
unless($tainted_dataset =~ m#^(\w{11})$#)
{
    handleerror("Dataset $tainted_dataset is not valid");
}
my $dataset = $1;

# untaint the aqcuisition date
unless($tainted_acqDate =~ m#^([\w.-]+)$#)
{
    handleerror("Acquisition Date $tainted_acqDate is not valid");
}
my $acqDate = $1;

if ($acqDate !~ m/^\d{2}-\d{2}-\d{4}$/)
{
    print header(), start_html("Acquisition Date Error"),
          p("Acquisistion Date $acqDate is not legal"),
          end_html(), "\n";
    exit;
}

# untaint the LLLong Point
unless($tainted_LLLong =~ m#^([\w.-]+)$#)
{
    handleerror("LL Long $tainted_LLLong is not valid");
}

my $LLLong = $1;

if ($LLLong !~ m/^\X{8,12}?$/)
{
    handleerror("LLLong Point $LLLong is not legal"); # exits
}

# untaint the URLong Point
unless($tainted_URLong =~ m#^([\w.-]+)$#)
{
    handleerror("UR Long $tainted_URLong is not valid");
}

my $URLong = $1;

if ($URLong !~ m/^\X{8,12}?$/)
{
    handleerror("URLong Point $URLong is not legal"); # exits
}

# untaint the ULLat Point
unless($tainted_ULLat =~ m#^([\w.-]+)$#)
{
    handleerror("UL Lat $tainted_ULLat is not valid");
}

my $ULLat = $1;

if ($ULLat !~ m/^\X{8,12}?$/)
{
    handleerror("ULLat Point $ULLat is not legal"); # exits
}

# untaint the LR Lat Point
unless($tainted_LRLat =~ m#^([\w.-]+)$#)
{
    handleerror("LR Lat $tainted_LRLat is not valid");
}

my $LRLat = $1;

if ($LRLat !~ m/^\X{8,12}?$/)
{
    handleerror("LRLat Point $LRLat is not legal"); # exits
}

# Because of the two different MRLC database tables for MRLC 2001 terrain
# corrected and 2001 reflectance adjusted there are two different directories
# that the data is stored. MRLC 2001 Terrain (mrlc_2001_tc, and MRLC 2001
# Reflectance (mrlc_2001_ra). 
# Using this information to be used for the different web page display depending
# on what sensor we are displaying.
my $sensor;
my $title;
my $abstract;
my $metadata_date;
if ($dataset eq "MRLC2K_ARCH")
{
    $sensor = "MRLC 2001 Terrain Corrected";
    $abstract = "MRLC 2001 TC Product: This data is a second generation "."
    updated collection of terrain-corrected Landsat 7 ETM+ and limited ".
    "Landsat 5 TM scenes covering the conterminous U.S., Alaska, and ".
    "Hawaii. The scenes are primarily 2000 imagery, although individual".
    " dates may range from 1999 to present. The data will also include ".
    "a 30-meter Digital Elevation Model (DEM) for all scenes that do not".
    " include international (Mexico or Canada) borders. Note: All ".
    "parameters are fixed and cannot be changed.   ";
    $metadata_date ="March 30, 2005";
}
elsif ($dataset eq "MRLC2K_SITC")
{
    $sensor = "MRLC 2001 At-Sensor Reflectance";
    $abstract ="MRLC 2001 RA Product: This data is a second generation ".
    "updated collection of terrain-corrected Landsat 7 ETM+ and limited ".
    "Landsat 5 TM scenes covering the conterminous U.S., Alaska, and Hawaii.".
    " The scenes are primarily 2000 imagery, although individual dates may ".
    "range from 1999 to present and the DN has been converted to at-satellite".
    " reflectance to correct for Sun illumination angle effect. The data will".
    " includes a 30-meter Digital Elevation Model (DEM) for all scenes that do".
    " not include international (Mexico or Canada) borders. Note: All".
    " parameters are fixed and cannot be changed.  ";
    $metadata_date ="March 30, 2005";
}
# extract the components needed from the scene id to build the path to the 
# actual image - MRLC can have different formats for the Entity ID
my $path = 0;
my $row = 0;
if ($sceneid =~ /L[ET]\d{14}[A-Z]{3}\d{2}/) # just like a Landsat ID
{
    $path = substr($sceneid,3,3);
    $row = substr($sceneid,6,3);
}
elsif ($sceneid =~ /[A-Z]{3}\w\d{17}/) # or an MRLC ID
{
    $path = substr($sceneid,5,3);
    $row = substr($sceneid,8,3);
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
    print start_html("USGS Global Visualization Viewer - Error");
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
<dt><b>Purpose:</b> These data sets were created with the intent to 
produce a national landcover "derivative" map product, thereby, 
stimulating broader use of Landsat data by the land data user community.
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
<dt><b>West_Bounding_Coordinate:</b> $LLLong</dt>
<dt><b>East_Bounding_Coordinate:</b> $URLong</dt>
<dt><b>North_Bounding_Coordinate:</b> $ULLat</dt>
<dt><b>South_Bounding_Coordinate:</b> $LRLat</dt>
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
<dt><b>Theme_Keyword:</b> MRLC</dt>
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
Geological Survey and the MRLC Consortium would be appreciated in 
products derived from this data.</dt>
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

