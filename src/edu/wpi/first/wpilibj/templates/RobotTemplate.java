//CAN TIMEOUTS EDITION
/*----------------------------------------------------------------------------*/
/* Copyright (c) FIRST 2008. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

//CHANGE NUMBERS SO THAT THE PORTS ARE CORRECT, no change for climbing logic yet, add second motor CODY
package edu.wpi.first.wpilibj.templates;


import edu.wpi.first.wpilibj.CANJaguar;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStationEnhancedIO;
import edu.wpi.first.wpilibj.Gyro;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.Victor;
import edu.wpi.first.wpilibj.can.CANTimeoutException;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;


public class RobotTemplate extends IterativeRobot 
{ 
    //Drive System
    CANJaguar leftMotor; //ATTENTION turn these victors back into jaguars
    CANJaguar rightMotor;
    RobotDrive drive;
    Joystick stick;
    Joystick wheel;
    Joystick copilot;
    JoystickButton leftTick;
    JoystickButton rightTick;
    
    
    //Shooter
    JoystickButton shootOn;
    JoystickButton shootOff;
    Tongue tongue;
    JoystickButton fire;
    Shooter shooter;
    Hopper hopper;
    boolean shooting = false;
    
    //Climbing
    JoystickButton upPart;
    JoystickButton upMax;
    JoystickButton down;
    JoystickButton autoClimb;
    DriverStationEnhancedIO driverStationButtons = DriverStation.getInstance().getEnhancedIO();
    Compressor comp; 
    ClimbingSystem climb;
    byte sg = 2;
    
    //Autonomous Crap
    Gyro gyro;
        
    PIDController pid;
    Output out;
    JoystickButton test;
    
    //State variables
    int hopperState = 0;
    int autoHopperState = 0;
    int hopperTimeout = 0;

    
    ErrorHandler errHandler = ErrorHandler.getErrorHandler();
    
    
    public void robotInit() 
    {
        try 
        {
            //Joystick Constructors
            wheel       = new Joystick(Wiring.WHEEL);
            stick       = new Joystick(Wiring.THROTTLE);
            copilot     = new Joystick(Wiring.COPILOT);
            
            //Drive Constructors
            leftMotor   = new CANJaguar(Wiring.LEFT_WHEEL);
            rightMotor  = new CANJaguar(Wiring.RIGHT_WHEEL);// JAG CHANGE
            leftMotor.setVoltageRampRate(Wiring.DRIVE_RAMP);
            rightMotor.setVoltageRampRate(Wiring.DRIVE_RAMP);
            drive       = new RobotDrive(leftMotor, rightMotor);
            drive.setInvertedMotor(RobotDrive.MotorType.kRearLeft, true);
            drive.setInvertedMotor(RobotDrive.MotorType.kRearRight, true);
            
            leftMotor.configNeutralMode(CANJaguar.NeutralMode.kBrake);
            rightMotor.configNeutralMode(CANJaguar.NeutralMode.kBrake);
           ;
            
           
            
            //Climber Constructors
            upPart      = new JoystickButton(Wiring.CLIMB_UP_PART);
            upMax       = new JoystickButton(Wiring.CLIMB_UP_MAX);
            down        = new JoystickButton(Wiring.CLIMB_DOWN);
            autoClimb   = new JoystickButton(Wiring.AUTO_CLIMB);
            climb       = new ClimbingSystem(this);
            comp        = new Compressor(8,1);
            comp.start();
            climb.goDownManual(1); // **ATTENTION** take out at RIT in order to pass rules
            
            //Shooter Constructors
            shootOn     = new JoystickButton(stick, 3);
            shootOff    = new JoystickButton(stick, 2);
            fire        = new JoystickButton(stick, 1);
            shooter     = new Shooter(Wiring.SHOOTER_MOTOR, stick);
            hopper      = new Hopper(Wiring.HOPPER_MOTOR);
            tongue      = new Tongue();
            
            //Autonomous Stuff
//            autonomousA = new DigitalInput(Wiring.AUTONOMOUS_SWITCH_A);
//            autonomousB = new DigitalInput(Wiring.AUTONOMOUS_SWITCH_B);
            gyro        = new Gyro(Wiring.GYRO_ANALOG);
            out         = new Output();
            pid         = new PIDController(Wiring.P, Wiring.I, Wiring.D, gyro, out);
            pid.setAbsoluteTolerance(1);
            test = new JoystickButton(Wiring.XBOX_Y_BUTTON);
            SmartDashboard.putBoolean("Shooter Up To Speed", false);

        } 
        catch (CANTimeoutException ex) 
        {
            ex.printStackTrace();
            System.out.println("CAN Time Out");//JAG CHANGE
        }
    }

    public void autonomousInit()
    {
       
    }

    public void autonomousPeriodic() 
    {
    
  
      
        if(!shooter.atSpeed())
        {
            
            shooter.shoot();
        }
        else
        {
            System.out.println("hopper");
            if(autoLoad())
            {
                Timer.delay(1.5);
            }
            
        }
    }

    public void teleopInit()
    {
         leftTick = new JoystickButton(stick, 10);
        rightTick = new JoystickButton(stick, 11);
        
    }
    
    public void teleopPeriodic() 
    {
        try {
            driveCheck(leftTick, rightTick);
            climb.iterativeCheck();
            shooterCheck();
            driveCheck(leftTick, rightTick);
            if(!driverStationButtons.getDigital(Wiring.AUTO_CLIMB))
            {
                try {
                    climb.winch.setX(.75, sg);
                    climb.winch2.setX(.75, sg);
                    climb.winch.updateSyncGroup(sg);
                } catch (CANTimeoutException ex) {
                    ex.printStackTrace();
                    System.out.println("CAN Time Out");
                }
          /////////////well commented     
            }
            if(stick.getRawButton(5))
            {
                tongue.lick();
            }else
            {
                tongue.slurp();
            }
        } catch (DriverStationEnhancedIO.EnhancedIOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void driveCheck(JoystickButton left, JoystickButton right)
    {
        drive.arcadeDrive(stick);
        if(leftTick.debouncedValue())
            {
                try {
                    rightMotor.setX(-.75);
                    leftMotor.setX(-.75);
                } catch (CANTimeoutException ex) {
                    ex.printStackTrace();
                    System.out.println("CAN Time Out");
                }
                Timer.delay(Wiring.TURN_DELAY);
                drive.arcadeDrive(0.0, 0.0);
            }
        else if(rightTick.debouncedValue())
            {
                try {
                    rightMotor.setX(.75);
                    leftMotor.setX(.75);
                } catch (CANTimeoutException ex) {
                    ex.printStackTrace();
                    System.out.println("CAN Time Out");
                }
                Timer.delay(Wiring.TURN_DELAY);
                drive.arcadeDrive(0.0, 0.0);
            }
    }
    
    public void shooterCheck()
    {   
        // logic for shooter motor control
        if (shootOn.debouncedValue())
        {
            shooting = true;
            shooter.shoot();
        }
        else if (shootOff.debouncedValue())
        {
            shooter.stop();
            shooting = false;
        }

        // shoot if not already pressed down
//        if(shooting)
//        {
//            //shooter.shoot();
//        }
//        else
//        {
//            //shooter.stop();
//        }

        //semi automatic shooting system
        //System.out.println("Loading!");
        load(); 
    }
    
    public void disabledPeriodic()
    {
        hopperState = 0;
        hopper.hopper.set(0);
        
    }
    
    public void disabledInit()
    {
        
    }
    
    public boolean shouldAbort() 
    {
        try 
        {
            if(!isEnabled() || !driverStationButtons.getDigital(Wiring.CLIMB_ON) )
            {
                errHandler.error("ABORTING ALL CLIMBING OPERATIONS");
                return true;
            }
            
        }
        catch (DriverStationEnhancedIO.EnhancedIOException ex) 
        {
            ex.printStackTrace();
        }
        return false;
    }
    
    public void load()
    {
        //System.out.println(hopperState);
        switch (hopperState)
        {
            case 0:
                hopper.hopper.set(0);
                if(fire.debouncedValue())
                {
                    hopperState = 1;
                    hopper.load();
                }
                break;
            case 1:
                hopperTimeout++;
                if(hopper.mag.get() && hopperTimeout < 100)
                {
                    hopperState = 2;
                    hopperTimeout = 0;
                }
                break;
            case 2:
                hopperTimeout++;
                if(!hopper.mag.get() && hopperTimeout < 100)
                {
                    hopperState = 0;
                    hopperTimeout = 0;
                    hopper.hopper.set(0.0);
                }
                break;
            default:
                hopperState = 0;
                break;
        }
    }
    public boolean autoLoad()
    {
        switch (autoHopperState)
        {
            case 0:
                hopper.hopper.set(0);
                    autoHopperState = 1;
                    hopper.load();
                    System.out.println("case 0");
                
                break;
            case 1:
                System.out.println("case 1");
                if(hopper.mag.get())
                {
                    autoHopperState = 2;
                }
                break;
            case 2:
                System.out.println("case2");
                if(!hopper.mag.get())
                {
                    autoHopperState = 0;
                    hopper.hopper.set(0.0);
                    return true;
                }
                break;
                
                
        }
        return false;
    }
}
