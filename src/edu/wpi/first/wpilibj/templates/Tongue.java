/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.wpi.first.wpilibj.templates;

import edu.wpi.first.wpilibj.Solenoid;


public class Tongue {
    
    Solenoid out;
    Solenoid in;
    
    public Tongue(){
        out = new Solenoid(Wiring.TONGUE_OUT);
        in = new Solenoid(Wiring.TONGUE_IN);
    }
    //lick is out, slurp is in
    public void lick(){
        in.set(false);
        out.set(true);
    }
    
    public void slurp(){
        in.set(true);
        out.set(false);
    }
}
