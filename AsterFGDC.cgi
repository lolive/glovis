#!/usr/bin/perl -wT
# This cgi script builds a web page to show the metadata for the list of
# scene id's passed in

# use strict mode
use strict;

# use the CGI module
use CGI qw(:standard escapeHTML);

# get the scene id passed in
my $taintedsceneid = param('scene_id');
my $taintedpath = param('path');
my $taintedrow = param('row');
my $taintedbands = param('bands');
my $tainted_acqDate = param('acq_date');
my $tainted_LLLong = param('LLLong');
my $tainted_URLong = param('URLong');
my $tainted_ULLat = param('ULLat');
my $tainted_LRLat = param('LRLat');
my $sceneid;
my $path;
my $row;
my $sensor;

# scene id is untainted
if ($taintedsceneid =~ /^(\d{3}):(\d{6,10})$/)
{
    $sceneid = $2;
}
elsif ($taintedsceneid =~ /^(\d{6,10})$/)
{
    $sceneid = $1;
}
else
{
    print header(), start_html("Scene ID Error"),
          p("Scene ID $taintedsceneid is not legal"),
          end_html(), "\n";
    exit;
}

# untaint the path and row
if ($taintedpath =~/^(\d{1,3})$/)
{
    $path = $1;
    $path = sprintf("%03d", $path);
}
else
{
    print header(), start_html("Scene ID Error"),
          p("Path $taintedpath is not legal"),
          end_html(), "\n";
    exit;
}
if ($taintedrow =~/^(\d{1,3})$/)
{
    $row = $1;
    $row = sprintf("%03d", $row);
}
else
{
    print header(), start_html("Scene ID Error"),
          p("Row $taintedrow is not legal"),
          end_html(), "\n";
    exit;
}

# untaint the band Available
unless($taintedbands =~ m#^([\w.-]+)$#)
{
    handleerror("Bands $taintedBands is not valid");
}
my $bands = $1;

if ($bands !~ m/^\w_\w_\w_\w_\w_\w_\w_\w_\w_\w_\w_\w_\w_\w_\w$/)
{
    handleerror("Bands Available $bands is not legal"); # exits
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

# untaint the LL Long Point
unless($tainted_LLLong =~ m#^([\w.-]+)$#)
{
    handleerror("LL Long $tainted_LLLong is not valid");
}
my $LLLong = $1;

if ($LLLong !~ m/^\X{8,12}?$/)
{
    handleerror("LLLong Point $LLLong is not legal"); # exits
}

# untaint the UR Long Point
unless($tainted_URLong =~ m#^([\w.-]+)$#)
{
    handleerror("UR Long $tainted_URLong is not valid");
}
my $URLong = $1;

if ($URLong !~ m/^\X{8,12}?$/)
{
    handleerror("URLong Point $URLong is not legal"); # exits
}

# untaint the UL Lat Point
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

# Using this information to be used for the different web page display depending
# on what sensor we are displaying.
my ($band1,$band2,$band3N,$band3B,$band4,$band5,$band6,$band7,$band8,
    $band9,$band10,$band11,$band12,$band13,$band14) = split(/_/,$bands);
$band1 = Yes_No($band1);
$band2 = Yes_No($band2);
$band3N = Yes_No($band3N);
$band3B = Yes_No($band3B);
$band4 = Yes_No($band4);
$band5 = Yes_No($band5);
$band6 = Yes_No($band6);
$band7 = Yes_No($band7);
$band8 = Yes_No($band8);
$band9 = Yes_No($band9);
$band10 = Yes_No($band10);
$band11 = Yes_No($band11);
$band12 = Yes_No($band12);
$band13 = Yes_No($band13);
$band14 = Yes_No($band14);

if ($row <= 124)
{
    $sensor = "VNIR"
}
else
{
    $sensor = "TIR"
}

my $title = "ASTER $sensor Product";
my $abstract = "ASTER L1A uncorrect product: ";
my $metadata_date = "May 23, 2006";
$title = " ASTER $sensor - Path: $path Row: $row ".
         "for Scene $sceneid";

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
# routine to convert yes = Y No = N flags into a full word description
sub Yes_No
{
    $_ = $_[0];
    return "Yes, band is acquired" if (m/^[Y|y]/);
    return "No, band was not acquired"if (m/^[N|n]/);
    return "Unavailable, band information not available"if (m/^[U|u]/);
    return $_;
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
<dt><b>Series_Name:</b> ASTER </dt>
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
National Map and with the intent of stimulating broader use of ASTER data 
by the land data user community.
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
<dt><b>Band Availability:</b></dt>
<dd>
<dl>
<dt><b>Band1_Availability:</b> $band1</dt>
<dt><b>Band2_Availability:</b> $band2</dt>
<dt><b>Band3N_Availability:</b> $band3N</dt>
<dt><b>Band3B_Availability:</b> $band3B</dt>
<dt><b>Band4_Availability:</b> $band4</dt>
<dt><b>Band5_Availability:</b> $band5</dt>
<dt><b>Band6_Availability:</b> $band6</dt>
<dt><b>Band7_Availability:</b> $band7</dt>
<dt><b>Band8_Availability:</b> $band8</dt>
<dt><b>Band9_Availability:</b> $band9</dt>
<dt><b>Band10_Availability:</b> $band10</dt>
<dt><b>Band11_Availability:</b> $band11</dt>
<dt><b>Band12_Availability:</b> $band12</dt>
<dt><b>Band13_Availability:</b> $band13</dt>
<dt><b>Band14_Availability:</b> $band14</dt>
</dd>
</dl>
<dt><b>Keywords:</b></dt>
<dd>
<dl>
<dt><b>Theme:</b></dt>
<dd>
<dl>
<dt><b>Theme_Keyword_Thesaurus:</b> none</dt>
<dt><b>Theme_Keyword:</b> LP DAAC</dt>
<dt><b>Theme_Keyword:</b> ASTER</dt>
<dt><b>Theme_Keyword:</b> Vegetation</dt>
<dt><b>Theme_Keyword:</b> Agriculture</dt>
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
<dt><b>Use_Constraints:</b> None.  Acknowledgement of the U.S. Geological 
Survey and the Land Processes Distributed Active Center (LP DAAC - estabilished 
as part of NASA's Earth Observing System (EOS) Data and Information System 
(EOSDIS) would be appreciated in products derived from this data.</dt>
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
<dt><b>Contact_Person:</b> LP DAAC Customer Service Representative</dt>
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
<dt><b>Contact_Voice_Telephone:</b> 605-594-6116 or 866-573-3222 
(tollfree)</dt>
<dt><b>Contact_Facsimile_Telephone:</b> 605-594-6589</dt>
<dt><b>Contact_Electronic_Mail_Address:</b> lpdaac\@usgs.gov</dt>
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
<dt><b>Contact_Person:</b> LP DAAC Customer Service Representative</dt>
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
<dt><b>Contact_Voice_Telephone:</b> 605-594-6116 or 866-573-3222 
(toll free) </dt>
<dt><b>Contact_Facsimile_Telephone:</b> 605-594-6589 </dt>
<dt><b>Contact_Electronic_Mail_Address:</b> lpdaac\@usgs.gov</dt>
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

