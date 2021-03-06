package org.firstinspires.ftc.teamcode.autonomous.gyroautos;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.RelicRecoveryVuMark;
import org.firstinspires.ftc.teamcode.compautonomous.Settings;
import org.firstinspires.ftc.teamcode.robotplus.autonomous.VuforiaWrapper;
import org.firstinspires.ftc.teamcode.robotplus.hardware.ColorSensorWrapper;
import org.firstinspires.ftc.teamcode.robotplus.hardware.GrabberPrimer;
import org.firstinspires.ftc.teamcode.robotplus.hardware.IMUWrapper;
import org.firstinspires.ftc.teamcode.robotplus.hardware.MecanumDrive;
import org.firstinspires.ftc.teamcode.robotplus.hardware.Robot;

/**
 * Red Left Scenario
 * @author Alex M, Blake A
 * @since 12/30/17
 */

@Autonomous(name="Gyro Red Left", group="gyro")
//@Disabled
public class GyroRL extends LinearOpMode implements Settings {

    private Robot robot;
    private DcMotor raiser;
    private Servo grabber;
    private MecanumDrive drivetrain;
    private IMUWrapper imuWrapper;
    private VuforiaWrapper vuforiaWrapper;
    private GrabberPrimer grabberPrimer;

    private Servo armExtender;
    private Servo armRotator;
    private ColorSensorWrapper colorSensorWrapper;

    private RelicRecoveryVuMark relicRecoveryVuMark;

    @Override
    public void runOpMode() throws InterruptedException {

        //Initialize hardware
        robot = new Robot(hardwareMap);
        drivetrain = (MecanumDrive) robot.getDrivetrain();
        raiser = hardwareMap.dcMotor.get("raiser");
        grabber = hardwareMap.servo.get("grabber");
        imuWrapper = new IMUWrapper(hardwareMap);
        vuforiaWrapper = new VuforiaWrapper(hardwareMap);
        grabberPrimer = new GrabberPrimer(this.grabber);

        //Assuming other hardware not yet on the robot
        armRotator = hardwareMap.servo.get("armRotator");
        armExtender = hardwareMap.servo.get("armExtender");

        armRotator.scaleRange(0.1, 0.9);
        armExtender.scaleRange(0.16, 0.85);

        armExtender.setPosition(1.0);
        armRotator.setPosition(0.5);

        this.grabberPrimer.initSystem();

        colorSensorWrapper = new ColorSensorWrapper(hardwareMap);

        vuforiaWrapper.getLoader().getTrackables().activate();

        waitForStart();

        this.grabberPrimer.grab();

        //STEP 1: Scan vuforia pattern
        relicRecoveryVuMark = RelicRecoveryVuMark.from(vuforiaWrapper.getLoader().getRelicTemplate());

        if (relicRecoveryVuMark != RelicRecoveryVuMark.UNKNOWN) {
            telemetry.addData("VuMark Column", relicRecoveryVuMark.name());
        } else {
            telemetry.addData("VuMark Column", "borked");
        }
        telemetry.update();

        //STEP 2: Hitting the jewel
        armExtender.setPosition(0); //servo in 'out' position

        sleep(2000);

        telemetry.addData("Color Sensor", "R: %f \nB: %f ", colorSensorWrapper.getRGBValues()[0], colorSensorWrapper.getRGBValues()[2]);
        //Checks that blue jewel is closer towards the cryptoboxes (assuming color sensor is facing forward
        if (Math.abs(colorSensorWrapper.getRGBValues()[2] - colorSensorWrapper.getRGBValues()[0]) < 30) {
            telemetry.addData("Jewels", "Too close.");
        } else if (colorSensorWrapper.getRGBValues()[2] > colorSensorWrapper.getRGBValues()[0]) {
            armRotator.setPosition(1);
            telemetry.addData("Jewels", "Blue Team!");
        } else {
            armRotator.setPosition(0);
            telemetry.addData("Jewels", "Red Team!");
        }
        telemetry.update();

        sleep(1000);

        armExtender.setPosition(1);
        armRotator.setPosition(0.5);

        sleep(1000);

        imuWrapper.getIMU().initialize(imuWrapper.getIMU().getParameters());

        // move backwards and slam into the wall
        this.drivetrain.complexDrive(MecanumDrive.Direction.DOWN.angle(), 1, 0); // move backwards
        sleep(firstStretch - 200);

        setAngle(imuWrapper, drivetrain, (float)Math.PI/2);
        sleep(1000);

        boolean kill = false;

        /*switch (relicRecoveryVuMark) {
            case LEFT: telemetry.addData("Column", "Putting it in the left");
                drivetrain.complexDrive(MecanumDrive.Direction.LEFT.angle(), 0.75, 0);
                sleep((long)(sideShort) - 80);
                drivetrain.complexDrive(MecanumDrive.Direction.UP.angle(), 0.75, 0);
                sleep((long)(sideShort));
                break;
            case CENTER: telemetry.addData("Column", "Putting it in the center");
                drivetrain.complexDrive(0, 0.75, 0);
                sleep((long)(sideShort));
                drivetrain.complexDrive(MecanumDrive.Direction.UP.angle(), 0.75, 0);
                sleep((long)(sideShort));
                break;
            case RIGHT: telemetry.addData("Column", "Putting it in the right");
                drivetrain.complexDrive(MecanumDrive.Direction.RIGHT.angle(), 0.75, 0);
                sleep((long)(sideShort));
                break;
            default:
                kill = true;
                drivetrain.complexDrive(MecanumDrive.Direction.UP.angle(), 0.75, 0);
                sleep((long)(sideShort));
                break;
        }*/

        drivetrain.complexDrive(MecanumDrive.Direction.UP.angle(), 0.75, 0);
        sleep((long)(sideShort));
        this.drivetrain.stopMoving();

        sleep(3000);

        this.drivetrain.stopMoving();

        this.grabberPrimer.open();
        sleep(1200);

        // pull away
        this.drivetrain.complexDrive(MecanumDrive.Direction.DOWN.angle(), 1, 0);
        sleep(100);
        this.drivetrain.stopMoving();
    }

    public void setAngle(IMUWrapper imuWrapper, MecanumDrive drivetrain, float angle){

        float heading = imuWrapper.getOrientation().toAngleUnit(AngleUnit.RADIANS).firstAngle;

        while (!(heading > angle - 0.1 && heading < angle + 0.1 )){
            if (heading > angle) {
                drivetrain.complexDrive(0, 0, 0.2);
            } else {
                drivetrain.complexDrive(0, 0, -0.2);
            }
            heading = imuWrapper.getOrientation().toAngleUnit(AngleUnit.RADIANS).firstAngle;
        }

        drivetrain.stopMoving();

    }
}
