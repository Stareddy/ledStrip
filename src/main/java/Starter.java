import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Starter {

    private static GpioPinDigitalOutput upstairsSensorTriggerPin;
    private static GpioPinDigitalInput upstairsSensorEchoPin;

    private static GpioPinDigitalOutput downstairsSensorTriggerPin;
    private static GpioPinDigitalInput downstairsSensorEchoPin;

    private final static GpioController gPIO = GpioFactory.getInstance();

    public static void main(String[] args) throws InterruptedException {
        new Starter().run();
    }

    private void run() throws InterruptedException {
        final boolean[] flag = {false};

        upstairsSensorTriggerPin = gPIO.provisionDigitalOutputPin(RaspiPin.GPIO_00);
        upstairsSensorEchoPin = gPIO.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_DOWN);

        downstairsSensorTriggerPin = gPIO.provisionDigitalOutputPin(RaspiPin.GPIO_09);
        downstairsSensorEchoPin = gPIO.provisionDigitalInputPin(RaspiPin.GPIO_07, PinPullResistance.PULL_DOWN);

        while (true) {
            if (isEarlyMorning() || isLateNight()) {
                try {
                    long distanceFromUpperSensor = getDistanceFromSensor(upstairsSensorTriggerPin, upstairsSensorEchoPin, getCurrentMinute(), getCurrentSeconds(), "upstairs");

                    if (distanceFromUpperSensor <= getUpperDistance() && !flag[0] && (isEarlyMorning() || isLateNight())) {
                        flag[0] = true;
//                        System.out.println("Upstairs - Sensor is on. "
//                                + "\n The distance is: " + distanceFromUpperSensor
//                                + "\n Current hour is: " + getCurrentHour()
//                                + "\n earlyMorning is: " + isEarlyMorning()
//                                + "\n and lateNight is: " + isLateNight()
//                                + "\n at: " + getTime());
                        try {
                            Process p = Runtime.getRuntime().exec("python relay_from_upstairs.py");
                            p.waitFor();
                            flag[0] = false;
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    long distanceFromLowerSensor = getDistanceFromSensor(downstairsSensorTriggerPin, downstairsSensorEchoPin, getCurrentMinute(), getCurrentSeconds(), "downstairs");

                    if (distanceFromLowerSensor <= getLowerDistance() && !flag[0] && (isEarlyMorning() || isLateNight())) {
                        flag[0] = true;
//                        System.out.println("Downstairs - Sensor is on. "
//                                + "\n The distance is: " + distanceFromLowerSensor
//                                + "\n Current hour is: " + getCurrentHour()
//                                + "\n earlyMorning is: " + isEarlyMorning()
//                                + "\n and lateNight is: " + isLateNight()
//                                + "\n at: " + getTime());
                        try {
                            Process p = Runtime.getRuntime().exec("python relay_from_downstairs.py");
                            p.waitFor();
                            flag[0] = false;
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private long getDistanceFromSensor(GpioPinDigitalOutput outputSensorTriggerPin, GpioPinDigitalInput inputSensorEchoPin, String currentMinute, String currentSeconds, String direction) throws InterruptedException {
        try {
            Thread.sleep(250);
            long minutes = Long.parseLong(currentMinute);
            long seconds = Long.parseLong(currentSeconds);
            long seconds2 = seconds + 1;

            outputSensorTriggerPin.high();
            Thread.sleep((long) 0.01);
            outputSensorTriggerPin.low();

            while (inputSensorEchoPin.isLow()) {
//                if (seconds < Long.parseLong(getCurrentSeconds()) || seconds == 59L) {
////                if (minutes < Long.parseLong(getCurrentMinute()) || minutes == 59L) {
//                    System.out.println("I am stuck in inputSensorEchoPin.isHigh() from " + direction + " in the " + minutes + " minute and " + seconds + " second. --> Second2: " + seconds2);
//                    return 99L;
//                }
            }

            long startTimeUpstairs = System.nanoTime();

            while (inputSensorEchoPin.isHigh()) {
//                if (seconds < Long.parseLong(getCurrentSeconds()) || seconds == 59L) {
////                if (minutes < Long.parseLong(getCurrentMinute()) || minutes == 59L) {
//                    System.out.println("I am stuck in inputSensorEchoPin.isHigh() from " + direction + " in the " + minutes + " minute and " + seconds + " second. --> Second2: " + seconds2);
//                    return 99L;
//                }
            }
            long endTimeUpstairs = System.nanoTime();

            return Math.round((((endTimeUpstairs - startTimeUpstairs) / 1e3) / 2) / 29.1);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return 99L;
        }
    }

    private static void runOldFunctionUsingPIR() throws InterruptedException {
        final boolean[] flag = {false};

        final GpioPinDigitalInput upperMovingSensor = gPIO.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_DOWN);

        upperMovingSensor.setShutdownOptions(true);

        upperMovingSensor.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState().equals(PinState.HIGH) && !flag[0] && (isEarlyMorning() || isLateNight())) {
                    flag[0] = true;
                    System.out.println("Upstairs - Sensor is on. Current hour is: " + getCurrentHour() + " earlyMorning is: " + isEarlyMorning() + " and lateNight is: " + isLateNight() + " at: " + getTime());
                    try {
                        Process p = Runtime.getRuntime().exec("python relay_from_upstairs.py");
                        p.waitFor();
                        flag[0] = false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        });

        final GpioPinDigitalInput lowerMovingSensor = gPIO.provisionDigitalInputPin(RaspiPin.GPIO_07, PinPullResistance.PULL_DOWN);

        lowerMovingSensor.setShutdownOptions(true);

        lowerMovingSensor.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if (event.getState().equals(PinState.HIGH) && !flag[0] && (isEarlyMorning() || isLateNight())) {
                    flag[0] = true;
                    System.out.println("Downstairs - Sensor is on. Current hour is: " + getCurrentHour() + "earlyMorning is: " + isEarlyMorning() + " and lateNight is: " + isLateNight() + " at: " + getTime());
                    try {
                        Process p = Runtime.getRuntime().exec("python relay_from_downstairs.py");
                        p.waitFor();
                        flag[0] = false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        System.out.println(" ... complete the GPIO #02 circuit and see the listener feedback here in the console.");

        while (true) {
            Thread.sleep(1000);
        }
    }

    private static String getTime() {
        SimpleDateFormat hm = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy");

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        return hm.format(timestamp);
    }

    private static String getCurrentHour() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH");

        return (String.valueOf(Integer.parseInt(sdf.format(new Date()))));
    }

    private static String getCurrentMinute() {
        SimpleDateFormat sdf = new SimpleDateFormat("mm");

        return (String.valueOf(Integer.parseInt(sdf.format(new Date()))));
    }

    private static String getCurrentSeconds() {
        SimpleDateFormat sdf = new SimpleDateFormat("ss");

        return (String.valueOf(Integer.parseInt(sdf.format(new Date()))));
    }

    private static boolean isLateNight() {

        ResultSet hours = getHours();

        int setFirstEveningTimeInDB = 0;
        int setSecondEveningTimeInDB = 0;

        try {
            assert hours != null;
            hours.next();
            setFirstEveningTimeInDB = hours.getInt("evening_1");
            setSecondEveningTimeInDB = hours.getInt("evening_2");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH");

        int currentHour = Integer.parseInt(sdf.format(new Date()));

        return (currentHour >= setFirstEveningTimeInDB && currentHour <= setSecondEveningTimeInDB);
    }

    private static boolean isEarlyMorning() {

        ResultSet hours = getHours();

        int setFirstMorningTimeInDB = 0;
        int setSecondMorningTimeInDB = 0;

        try {
            assert hours != null;
            hours.next();
            setFirstMorningTimeInDB = hours.getInt("morning_1");
            setSecondMorningTimeInDB = hours.getInt("morning_2");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH");

        int currentHour = Integer.parseInt(sdf.format(new Date()));

        return (currentHour >= setFirstMorningTimeInDB && currentHour <= setSecondMorningTimeInDB);
    }

    private static ResultSet getHours() {
        try {
            Class.forName("com.mysql.jdbc.Driver");

            Connection conn = DriverManager
                    .getConnection(URI, userName, passWord);

            Statement statement = conn.createStatement();

            return statement
                    .executeQuery("select * from ledStrip.time");

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static ResultSet getDistance() {
        try {
            Class.forName("com.mysql.jdbc.Driver");

            Connection conn = DriverManager
                    .getConnection(URI, userName, passWord);

            Statement statement = conn.createStatement();

            return statement
                    .executeQuery("select * from ledStrip.distance");

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static long getUpperDistance() {

        ResultSet distance = getDistance();

        int upperDistance = 0;

        try {
            assert distance != null;
            distance.next();
            upperDistance = distance.getInt("upper");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return upperDistance;
    }

    private static long getLowerDistance() {

        ResultSet distance = getDistance();

        int lowerDistance = 0;

        try {
            assert distance != null;
            distance.next();
            lowerDistance = distance.getInt("lower");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lowerDistance;
    }
}