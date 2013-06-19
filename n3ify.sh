SENSORSFILE=sensors.n3


#--------------
#0: Inject prefix in SENSORSFILE
#--------------
echo '@prefix: <http://toto/#> . ' > $SENSORSFILE


#-------------
#1: To get the relative URIs of sensors, please apply this Unix command: 
#-------------

#Nhap:
for i in `ls SensorMenu.java`;do grep NhapDataset $i|sed 's/.*"\([^,]*\)", *"\([^,]*\)", *$/:\1 :sensorPath "\2" . /'|sed 's/ \([^:"\.]\)/_\1/g';done >> $SENSORSFILE

#Modis:
cat SensorMenu.java | sed 'N; s/.*ModisSensor.*([^,]*,"\([^,]*\)",[^,]*"\([^,]*\)",.*/:::\1 :sensorData "\2" . :\1 :subProjectOf :ModisSensor ./'|grep :::|sed 's/:::/:/'|sed 's/ \([^:"\.]\)'/_\1/g >> $SENSORSFILE

#All others:
for i in `grep -ls 'public class .* extends .*Sensor' *.java`;do echo -ne ":${i%\.java} :sensorData" && grep super $i|sed 's/\s*super *([^,]*,[^,]*,\s*"\([^,]*\)",.*/ "\1" . /';done |grep -v "super("|grep -v '""$' >> $SENSORSFILE


#--------------
#2: Get the makeImageName method
#--------------

#All:
for i in `grep -ls makeImageName *java`;do echo -ne ":${i%\.java} :javaCodeToImgUrl \"\"\"" && sed -n '/public String makeImageName/,/public/p' $i |tac|sed '1,/^ *} *$/ d'|tac && echo '}""" . ';done >> $SENSORSFILE

#--------------
#3: Get the subclasses of a subclass of sensors
#--------------

#All:
grep class.*extends.*Sensor *java|sed 's/\(.*\)\.java.*extends \(.*\)/:\1 :subProjectOf :\2 . /'|grep -v ' :Sensor . $' >> $SENSORSFILE
