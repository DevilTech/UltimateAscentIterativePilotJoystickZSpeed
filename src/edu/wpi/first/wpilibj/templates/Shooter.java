package edu.wpi.first.wpilibj.templates;

import edu.wpi.first.wpilibj.CANJaguar;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Victor;
import edu.wpi.first.wpilibj.can.CANTimeoutException;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;


public class Shooter 
{
    CANJaguar shoot;
    boolean hasSeenMax = false;
    final double aCHigh = 25;
    final double aCLow  = 19;
    boolean atMax;
    Joystick j;
    
    public Shooter(int port, Joystick joy)
    {
        try {
            shoot = new CANJaguar(port);
            shoot.setVoltageRampRate(Wiring.RAMP_VOLTS_PER_SECOND);
            j = joy;
        } catch (CANTimeoutException ex) {
            ex.printStackTrace();
            System.out.println("CAN Time Out");
        }
    }
    
    public void shoot()
    {
        //System.out.println("Shooting");
        try 
        {
            shoot.setX(caliZ());

        }
        catch (CANTimeoutException ex)
        {
            ex.printStackTrace();
            System.out.println("CAN Time Out");
        }
        try 
        {
            if(hasSeenMax && shoot.getOutputCurrent() < aCLow)
            {
                SmartDashboard.putBoolean("Shooter Up To Speed", true);
                atMax = true;
            }
            else if (shoot.getOutputCurrent() > aCHigh)
            {
                hasSeenMax = true;
            }
        } 
        catch (CANTimeoutException ex) {
            ex.printStackTrace();
            System.out.println("CAN Time Out");
        }
    }
    
    public void stop()
    {
        try {
            shoot.setX(0);
            hasSeenMax = false;
            atMax = false;
            SmartDashboard.putBoolean("Shooter Up To Speed", false);
        } catch (CANTimeoutException ex) {
            ex.printStackTrace();
            System.out.println("CAN Time Out");
        }
    }
    public boolean atSpeed(){
        if(!hasSeenMax){
            atMax = false;
        }
        return atMax;
    }
    public double caliZ(){
        return (1 - (j.getZ() * .5)); //MOST CURRENT CODE 9/5/2013, if we want to lower the lowest possible speed, change the .3 to .5 and test
        // changed value to .5 for hang around victor day, used z axis to dial down speed and adjust without redownloading
    }
}
