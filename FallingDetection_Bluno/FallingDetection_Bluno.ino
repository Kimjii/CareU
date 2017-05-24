/* 
 *  This Program is simple Falling Detection program.
 *  I use only two hardware, bluno and mpu6050 sensor. 
 *  
*/

#include <Wire.h>
#include "Kalman.h" // Source: https://github.com/TKJElectronics/KalmanFilter

Kalman kalmanX; // Create the Kalman instances
Kalman kalmanY;

/* IMU Data */
int16_t accX, accY, accZ;
int16_t tempRaw;
int16_t gyroX, gyroY, gyroZ;

double accXangle, accYangle; // Angle calculate using the accelerometer
double temp; // Temperature
double gyroXangle, gyroYangle; // Angle calculate using the gyro
double compAngleX, compAngleY; // Calculate the angle using a complementary filter
double kalAngleX, kalAngleY; // Calculate the angle using a Kalman filter

uint32_t timer;
uint8_t i2cData[14]; // Buffer for I2C data

/* Data related to Detect of Falling */
struct Value{
  float x;
  float y;
};
const float threshold1X = 20.0;
const float threshold1Y = 45.0;
const float threshold2X = 2.0;
const float threshold2Y = 1.0;

const int WINDOWSIZE = 25;
const int AFTERWINDOWSIZE = 50;

struct Value realtimeValues[WINDOWSIZE] = {0};
int count;
struct Value realtimeSum;
bool isShocked;

struct Value add( struct Value v1, struct Value v2 );
struct Value subtract( struct Value v1, struct Value v2 );
struct Value getDifferenceValue();
struct Value readValue();
struct Value calculateAverage( struct Value sum, int windowSize );
struct Value calculateSum( struct Value values[], int windowSize );
void display_formatted_float(double val, int characteristic, int mantissa, int blank, boolean linefeed);
int absolute(int val);

void setup() {
  Serial.begin(115200);
  Wire.begin();
  i2cData[0] = 7; // Set the sample rate to 1000Hz - 8kHz/(7+1) = 1000Hz
  i2cData[1] = 0x00; // Disable FSYNC and set 260 Hz Acc filtering, 256 Hz Gyro filtering, 8 KHz sampling
  i2cData[2] = 0x00; // Set Gyro Full Scale Range to ±250deg/s
  i2cData[3] = 0x00; // Set Accelerometer Full Scale Range to ±2g
  while(i2cWrite(0x19,i2cData,4,false)); // Write to all four registers at once
  while(i2cWrite(0x6B,0x01,true)); // PLL with X axis gyroscope reference and disable sleep mode
  while(i2cRead(0x75,i2cData,1));
  if(i2cData[0] != 0x68) { // Read "WHO_AM_I" register
    Serial.print(F("Error reading sensor"));
    while(1);
  }
  
  delay(100); // Wait for sensor to stabilize
  
  /* Set kalman and gyro starting angle */
  while(i2cRead(0x3B,i2cData,6));
  accX = ((i2cData[0] << 8) | i2cData[1]);
  accY = ((i2cData[2] << 8) | i2cData[3]);
  accZ = ((i2cData[4] << 8) | i2cData[5]);
  // atan2 outputs the value of -π to π (radians) - see http://en.wikipedia.org/wiki/Atan2
  // We then convert it to 0 to 2π and then from radians to degrees
  accYangle = (atan2(accX,accZ)+PI)*RAD_TO_DEG;
  accXangle = (atan2(accY,accZ)+PI)*RAD_TO_DEG;
  
  kalmanX.setAngle(accXangle); // Set starting angle
  kalmanY.setAngle(accYangle);
  gyroXangle = accXangle;
  gyroYangle = accYangle;
  compAngleX = accXangle;
  compAngleY = accYangle;
  
  timer = micros();

  /* Set Variable for Falling Detection */
  isShocked = false;
  realtimeSum.x = 0;
  realtimeSum.y = 0;
  
  count = 0;
  for( int i = 0; i < WINDOWSIZE; i++ )
      realtimeValues[i] = getDifferenceValue();
      
  realtimeSum = calculateSum( realtimeValues, WINDOWSIZE );
  
  Serial.print("Falling Detection Ready.");
}

void loop() {
  if( count == WINDOWSIZE ) 
    count = 0;

  /* Monitoring realtime sensor value applied Kalman */
  realtimeSum = subtract( realtimeSum, realtimeValues[count] );
  realtimeValues[count] = getDifferenceValue();
  realtimeSum = add( realtimeSum, realtimeValues[count++] );
  //Serial.print( "realtimeSumX : " ); display_formatted_float(realtimeSum.x, 5, 2, 3, false); Serial.print('|');
  //Serial.print( "realtimeSumY : " ); display_formatted_float(realtimeSum.y, 5, 2, 3, true);
    
  /* If last realtime values's difference are bigger than threshold1 that is situation of shocked.  */
  if( absolute(realtimeSum.x) > threshold1X && absolute(realtimeSum.y) > threshold1Y ){
    isShocked = true;
    Serial.print("Shocked!");
    Serial.print( "realtimeSumX : " ); display_formatted_float(realtimeSum.x, 5, 2, 3, false); Serial.print('|');
    Serial.print( "realtimeSumY : " ); display_formatted_float(realtimeSum.y, 5, 2, 3, true);
  }
    
  
  if( isShocked )
  {
    delay(1000);
    struct Value afterValues[AFTERWINDOWSIZE];
    for( int i = 0; i < AFTERWINDOWSIZE; i++ )
    {
      afterValues[i].x = 0;
      afterValues[i].y = 0;
    }
    
    for( int i = 0; i < AFTERWINDOWSIZE; i++ )
      afterValues[i] = getDifferenceValue();
      
    struct Value sum = calculateSum( afterValues, AFTERWINDOWSIZE );
    //Serial.print( "sumX : " ); display_formatted_float(sum.x, 5, 2, 3, false); Serial.print('|');
    //Serial.print( "sumY : " ); display_formatted_float(sum.y, 5, 2, 3, true);
    
    struct Value afterAvg = calculateAverage( sum, AFTERWINDOWSIZE );
    Serial.print( "afterAvgX : " ); display_formatted_float(afterAvg.x, 5, 2, 3, false); Serial.print('|');
    Serial.print( "afterAvgY : " ); display_formatted_float(afterAvg.y, 5, 2, 3, true);

    /* After shocked, if this situation was Falling that after read values are smaller than threshold2. */
    if( absolute( afterAvg.x ) < threshold2X && absolute( afterAvg.y ) < threshold2Y )  // Falling Detection
      Serial.print("Falling!!");

    for( int i = 0 ; i < WINDOWSIZE; i++ )
      realtimeValues[i] = afterValues[AFTERWINDOWSIZE-WINDOWSIZE+i];
      
    realtimeSum = calculateSum( realtimeValues, WINDOWSIZE );

    isShocked = false;
  } // end of if( isShocked )
}

struct Value add( struct Value v1, struct Value v2 )
{
  struct Value result;
  result.x = v1.x + v2.x;
  result.y = v1.y + v2.y;
  
  return result;
}

struct Value subtract( struct Value v1, struct Value v2 )
{
  struct Value result;
  result.x = v1.x - v2.x;
  result.y = v1.y - v2.y;

  return result;
}

struct Value getDifferenceValue()
{
  struct Value preValue = readValue();
  struct Value currValue = readValue();

  struct Value diffValue = subtract( preValue, currValue );
  
  return diffValue;
}

struct Value readValue()
{
    /* Update all the values */
  while(i2cRead(0x3B,i2cData,14));
  accX = ((i2cData[0] << 8) | i2cData[1]);
  accY = ((i2cData[2] << 8) | i2cData[3]);
  accZ = ((i2cData[4] << 8) | i2cData[5]);
  tempRaw = ((i2cData[6] << 8) | i2cData[7]);
  gyroX = ((i2cData[8] << 8) | i2cData[9]);
  gyroY = ((i2cData[10] << 8) | i2cData[11]);
  gyroZ = ((i2cData[12] << 8) | i2cData[13]);
  
  accXangle = (atan2(accY,accZ)+PI)*RAD_TO_DEG;
  accYangle = (atan2(accX,accZ)+PI)*RAD_TO_DEG;
  
  double gyroXrate = (double)gyroX/131.0;
  double gyroYrate = -((double)gyroY/131.0);
  gyroXangle += gyroXrate*((double)(micros()-timer)/1000000); // Calculate gyro angle without any filter
  gyroYangle += gyroYrate*((double)(micros()-timer)/1000000);
  //gyroXangle += kalmanX.getRate()*((double)(micros()-timer)/1000000); // Calculate gyro angle using the unbiased rate
  //gyroYangle += kalmanY.getRate()*((double)(micros()-timer)/1000000);
  
  compAngleX = (0.93*(compAngleX+(gyroXrate*(double)(micros()-timer)/1000000)))+(0.07*accXangle); // Calculate the angle using a Complimentary filter
  compAngleY = (0.93*(compAngleY+(gyroYrate*(double)(micros()-timer)/1000000)))+(0.07*accYangle);
  
  kalAngleX = kalmanX.getAngle(accXangle, gyroXrate, (double)(micros()-timer)/1000000); // Calculate the angle using a Kalman filter
  kalAngleY = kalmanY.getAngle(accYangle, gyroYrate, (double)(micros()-timer)/1000000);
  timer = micros();

  struct Value value;
  value.x = kalAngleX;
  value.y = kalAngleY;

  delay(1);
  return value;
}

struct Value calculateAverage( struct Value sum, int windowSize )
{
  struct Value average;
  average.x = sum.x / windowSize;
  average.y = sum.y / windowSize;
  
  return average;
}

struct Value calculateSum( struct Value values[], int windowSize )
{
  struct Value sum;
  sum.x = 0;
  sum.y = 0;
  
  for( int i = 0 ; i < windowSize; i++ ) {
    sum.x += values[i].x;
    sum.y += values[i].y;
  }
  
  return sum;
}

void display_formatted_float(double val, int characteristic, int mantissa, int blank, boolean linefeed) 
{
  char outString[16];
  int len;

  dtostrf(val, characteristic, mantissa, outString);
  len = strlen(outString);

  //Serial.print(outString);
  Serial.write(outString);
  
  if(linefeed)
    //Serial.print(F("\n"));
    Serial.write("\n");
}

int absolute(int val)
{
  if (val < 0)
    val *= -1;

  return val;
}
