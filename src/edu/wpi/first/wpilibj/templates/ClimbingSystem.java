package edu.wpi.first.wpilibj.templates;

import edu.wpi.first.wpilibj.CANJaguar;
import edu.wpi.first.wpilibj.Counter;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DriverStationEnhancedIO.EnhancedIOException;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.can.CANTimeoutException;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class ClimbingSystem
{
    Solenoid up;
    Solenoid forward;
    Solenoid back;
    Solenoid down;
    CANJaguar winch;
    CANJaguar winch2;
    DigitalInput home;
    DigitalInput part;
    DigitalInput max;
    RobotTemplate robo;
    Counter countPart;
    byte sg = 2;
    
    Timer time = new Timer();
    ErrorHandler errHandler = ErrorHandler.getErrorHandler();
    
    double typicalCurrent = 0;
 
    final double thresh_hold = 35.0;
    final double downspeed   =  1.00;
    final double downSpeedFast= 1.00;
    final double upspeed     = -1.00; //-.75
    
    int countState = 0;
    int posState = 2;
    int state = 0;
    boolean passHome = true;
    boolean hitHome = true;
    boolean isForward = true;
    public ClimbingSystem(RobotTemplate robo)            
    {
        try 
        {
            up = new Solenoid(Wiring.CLIMB_SOLENOID_UP);
            down = new Solenoid(Wiring.CLIMB_SOLENOID_DOWN);
            forward = new Solenoid(Wiring.CLIMBING_SOLENOID_FORWARD);
            back = new Solenoid(Wiring.CLIMBING_SOLENOID_BACKWARD);
            home = new DigitalInput(Wiring.HOME_LIMIT_SWITCH);
            part = new DigitalInput(Wiring.CYLINDER_PART);
            max = new DigitalInput(Wiring.CYLINDER_MAX);
            winch = new CANJaguar(Wiring.WINCH_MOTOR);
            winch2 = new CANJaguar(Wiring.WINCH_2);
            countPart = new Counter(part);
            countPart.setUpSourceEdge(true, false);
            countPart.reset();
            countPart.start();
            winch.configMaxOutputVoltage(6.0);
            winch2.configMaxOutputVoltage(6.0);
            forward.set(true);
            back.set(false);
            this.robo = robo;
            time.start();
            typicalCurrent = 0;
            
        } 
        catch (CANTimeoutException ex) 
        {
            ex.printStackTrace();
            System.out.println("CAN Time Out");
        }
    }
    
    public void autoClimbPartial(int button)
    {
       //System.out.println("Auto CLimb Part");
       autoClimb(false, button);
    }
    
    public void autoClimbMax(int button)
    {
        //System.out.println("Auto CLimb Max");
        autoClimb(true, button);
    }
    
    public void autoClimb(boolean max, int button)
    {
        int state = 0;
        
        while(!robo.shouldAbort() && state != 5)
        {
            //System.out.println(state);
            switch(state)
            {
                case 0:
                    goDownManual(button);
                    state = 1;
                    break;
                case 1:
                    goForward();
                    state = 2;
                    break;
                case 2:
                    if(!max)
                    {
                        goUpPartial(button);
                    }
                    else
                    {
                        goUpMax(button);
                    }
                    state = 3;
                    break;
                case 3:
                    goBackward();
                    state = 4;
                    break;
                case 4:
                    goDownManual(button);
                    state = 5;
                    break;
                default:
                    stop();
                    break;
            }
                
        }
        //System.out.println("****ABORTED*****");
    }
   
    public void goUpMax(int button)
    {         
        //System.out.println("Go Up Max");
        
        double curTime = time.get();
        try
        {
            if(!max.get())
            {
                errHandler.error("ALREADY AT MAX HEIGHT");
                return;
            }

            up.set(true);
            down.set(false);
            winch.setX(upspeed, sg);
            winch2.setX(upspeed, sg);
            winch.updateSyncGroup(sg);
            passHome = false;

                while(true)
                {
                    //SmartDashboard.putBoolean("AT HOME", !home.get());
                    if(robo.shouldAbort())
                    {
                        stop();
                        break;
                    }

                    // abort if stuck at home for more than 2 seconds
                    if(home.get() && time.get() > curTime + 2.0)
                    {
                        errHandler.error("STUCK AT HOME, WINCH DISABLED");
                        winch.setX(0.0, sg);
                        winch2.setX(0.0, sg);
                        winch.updateSyncGroup(sg);
                        break;
                    }

                    if(!max.get())
                    {
                        System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$ GOT TO MAX $$$$$$$$$$$$$$$$$$$$");
                        winch.setX(0.0, sg);
                        winch2.setX(0.0, sg);
                        winch.updateSyncGroup(sg);
                        break;
                    }  
                }
        }
        catch (CANTimeoutException ex) 
        {
            ex.printStackTrace();
            System.out.println("CAN Time Out");
        }
    }
    
    public void goUpPartial(int button) 
    {
        //System.out.println("Go Up Part");
        
        double curTime = time.get();
        try
        {
            if(!passHome)
            {      
                errHandler.error("NOT AT HOME, RETURN HOME BEFORE RAISING TO PART");
                return;
            }
            
            countState = countPart.get();

            up.set(true);
            down.set(false);
            winch.setX(upspeed, sg);
            winch2.setX(upspeed, sg);
            winch.updateSyncGroup(sg);
            
            while(true)
            {
                //SmartDashboard.putBoolean("AT HOME", !home.get());
                if(robo.shouldAbort())
                {
                    stop();
                    break;
                }

                //abort if stuck at home for more than 2 seconds
                if(home.get() && time.get() > curTime + 2.0)
                {
                    errHandler.error("STUCK AT HOME, WINCH DISABLED");
                    winch.setX(0.0, sg);
                    winch2.setX(0.0, sg);
                    winch.updateSyncGroup(sg);
                    break;
                }

                if(countPart.get()!= countState)
                {
                    System.out.println("$$$$$$$$$$$$$$$$$$$$$$ HIT PART $$$$$$$$$$$$$$$$$$$$$$4");
                    winch.setX(0.0, sg);
                    winch2.setX(0.0, sg);
                    winch.updateSyncGroup(sg);
                    break;
                }
                if(!max.get())
                {
                    errHandler.error("PASSED PART, AND REACHED MAX, RESET TO HOME");
                    winch.setX(0.0, sg);
                    winch2.setX(0.0, sg);
                    winch.updateSyncGroup(sg);
                    break;
                }
            }
        } 
        catch (CANTimeoutException ex) 
        {
            ex.printStackTrace();
            System.out.println("CAN Time Out");
        }
    }
    
    public void goDownManual(int button)
    {
        //System.out.println("Going Down");
        
        try
        {
            if(home.get())
            {
                //we are at home: Bail out
                errHandler.error("ALREADY AT HOME");
                return;
            }

            up.set(true);
            down.set(false);
            winch.setX(downspeed, sg);
            winch2.setX(upspeed, sg);
            winch.updateSyncGroup(sg);

            while(true)
            {
                //SmartDashboard.putBoolean("AT HOME", !home.get());
                //SmartDashboard.putNumber("Current", winch.getOutputCurrent());
                //SmartDashboard.putNumber("Encoder Ticks", winch.getPosition());
//                if(!home.get())
//                {
//                    SmartDashboard.putBoolean("Home", true);
//                }
//                if(count.get() == 1)
//                {
//                    SmartDashboard.putBoolean("Part", true);
//                }
//                if(!max.get())
//                {
//                    SmartDashboard.putBoolean("Max", true);
//                }
                
                if(robo.shouldAbort())
                {
                    
                    stop();
                    break;
                }
                if(home.get())
                {
                    time.delay(1.0);
                    System.out.println("$$$$$$$$$$$$$$$$$$$$$ GOT HOME $$$$$$$$$$$$$$$$$$$$$$$$$$");
                    up.set(false);
                    down.set(true);
                    winch.setX(0.0, sg);
                    winch2.setX(0.0, sg);
                    winch.updateSyncGroup(sg);
                    passHome = true;
                    break;
                }
                if(isHitBar())
                {
                    System.out.println("$$$$$$$$$$$$$$$$$$$$ HIT THE BAR $$$$$$$$$$$$$$$");
                    //**ATTENTION** adjust motor speed if needed to a higher speed
                    winch.setX(downSpeedFast, sg);
                    winch2.setX(downSpeedFast, sg);
                    winch.updateSyncGroup(sg);
                    up.set(false);
                    down.set(true);
                }
            }
        }
        catch (CANTimeoutException ex)
        {
            ex.printStackTrace();
            System.out.println("CAN Time Out");
        }
    }
    
    public void goForward()
    {
        //System.out.println("Forward");
        
        isForward = true;
        back.set(false);
        forward.set(true);
    }
    
    public void goBackward()
    {
        //System.out.println("Backward");
        isForward = false;
        forward.set(false);
        back.set(true);
    }
    
    public boolean isHitBar()
    {
        boolean hitbar = false;
        try 
        {       
            double value = winch.getOutputCurrent();
            
            //Is 2.0 B/C we don't want to sample too fast so it will never fall out of the 
            if (value <= 2.0) 
            {
                typicalCurrent = value;
            }
            else if (value >= (typicalCurrent + thresh_hold))
            {
                hitbar = true;
            }
            else
            {
                hitbar = false;
            }
        } 
        catch (CANTimeoutException ex) 
        {
            ex.printStackTrace();
            System.out.println("CAN Time Out");
        }
        return hitbar;
    }
    
    public void stop()
    {
        try {
            up.set(false);
            down.set(true);
            forward.set(true);
            back.set(false);
            winch.setX(0.0, sg);
            winch2.setX(0.0, sg);
            winch.updateSyncGroup(sg);
        } catch (CANTimeoutException ex) {
            ex.printStackTrace();
            System.out.println("CAN Time Out");
        }
    }
    
    public void iterativeCheck() throws EnhancedIOException
    {
//        System.out.println(state);
//        System.out.println(home.get());
         switch (state)
        {
            case 0: //Default in a stopped state
                 try 
                 {
                    
                    if(robo.driverStationButtons.getDigital(Wiring.CLIMB_ON))
                    {
                        state = 1;
                    }
                } 
                catch (EnhancedIOException ex) 
                {
                    ex.printStackTrace();
                }
                try 
                {
                    if(robo.shouldAbort() && hitHome)
                    {
                        up.set(false);
                        down.set(true);
                    }
                    else
                    {
                        up.set(true);
                        down.set(false);
                    }
                    
                    forward.set(true);
                    back.set(false);
                    winch.setX(0.0, sg);
                    winch2.setX(0.0, sg);
                    winch.updateSyncGroup(sg);
                } 
                catch (CANTimeoutException ex) 
                {
                    ex.printStackTrace();
                    System.out.println("CAN Time Out");
                }
               
                break;
            case 1: //Master switch is on, button checks
                if(robo.shouldAbort())
                {
                    state = 0;
                }
                else if(robo.down.debouncedValueDigital())
                {
                    if(home.get() && isForward)
                    {
                        state = 2;
                    }
                }
                else if(robo.upPart.debouncedValueDigital())
                {
                    if(passHome)
                    {
                        countState = countPart.get();
                        state = 4;
                    }
                }
                else if(robo.upMax.debouncedValueDigital())
                {
                    if(max.get())
                    {
                        state = 5;
                    }
                }
                else try 
                {
                    if(robo.driverStationButtons.getDigital(Wiring.FORWARD_BACK))
                   {
                       goBackward();
                   }
                   else
                   {
                       goForward();
                   }
                } 
                catch (EnhancedIOException ex) 
                {
                    ex.printStackTrace();
                }
                
                break;
            case 2://Pressed down button
                up.set(true);
                down.set(false);
                try 
                {
                    winch.setX(downspeed, sg);
                    winch2.setX(downspeed, sg);
                    winch.updateSyncGroup(sg);
                } 
                catch (CANTimeoutException ex) 
                {
                    ex.printStackTrace();
                    System.out.println("CAN Time Out");
                }
                if(robo.shouldAbort())
                {
                    state = 0;
                }
                else if(isHitBar())
                {
                    state = 3;
                }
                else if(!home.get())
                {
                    //time.delay(1.0);
                    passHome = true;
                    hitHome = true;
                    state = 0;
                }
                
                break;  
            case 3: //Hit the Bar, Go down Faster
                up.set(false);
                down.set(true);
                try 
                {
                    winch.setX(downSpeedFast, sg);
                    winch2.setX(downSpeedFast, sg);
                    winch.updateSyncGroup(sg);
                } 
                catch (CANTimeoutException ex) 
                {
                    ex.printStackTrace();
                    System.out.println("CAN Time Out");
                }
                if(robo.shouldAbort())
                {
                    state = 0;
                }
                else if(!home.get())
                {
                    //time.delay(1.0);
                    passHome = true;
                    hitHome = true;
                    state = 0;
                }
                break;
            case 4: //Hit Partial CLimb
                double curTime = time.get();
                
                up.set(true);
                down.set(false);
                hitHome = false;
                try 
                {
                    winch.setX(upspeed, sg);
                    winch2.setX(upspeed, sg);
                    winch.updateSyncGroup(sg);
                } 
                catch (CANTimeoutException ex) 
                {
                    ex.printStackTrace();
                    System.out.println("CAN Time Out");
                }
                if(robo.shouldAbort())
                {
                    state = 0;
                }
                else if(!home.get() && time.get() > curTime + 2.0)
                {
                    state = 0;
                }
                else if(countPart.get()!= countState)
                {
                    state = 0;
                }
                break;
            case 5: //Hit Max button
                curTime = time.get();
                passHome = false;
                up.set(true);
                down.set(false);
                hitHome = false;
                try 
                {
                    winch.setX(upspeed, sg);
                    winch2.setX(upspeed, sg);
                    winch.updateSyncGroup(sg);
                } 
                catch (CANTimeoutException ex) 
                {
                    ex.printStackTrace();
                    System.out.println("CAN Time Out");
                }
                if(robo.shouldAbort())
                {
                    state = 0;
                }
                else if(!home.get() && time.get() > curTime + 2.0)
                {
                    state = 0;
                }
                else if(!max.get())
                {
                    state = 0;
                }
                break;
            default:
                state = 0;
                break;
                
                
        }
    }
}