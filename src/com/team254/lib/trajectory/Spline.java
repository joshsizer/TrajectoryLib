package com.team254.lib.trajectory;

import com.team254.lib.util.ChezyMath;

/**
 * Do cubic spline interpolation between points.
 * @author Art Kalb
 * @author Jared341
 */
public class Spline {
  double a_;  // ax^3
  double b_;  // + bx^2
  double c_;  // + cx
  // + d (but d is always 0 in our formulation)
  
  // The offset from the world frame to the spline frame.
  // Add these to the output of the spline to obtain world coordinates.
  double y_offset_;
  double x_distance_;
  double theta_offset_;
  
  Spline() {
    // All splines should be made via the static interface
  }
  
  private static boolean almostEqual(double x, double y) {
    return Math.abs(x-y) < 1E-6;
  }
  
  public static boolean reticulateSplines(Path.Waypoint start,
          Path.Waypoint goal, Spline result) {
    return reticulateSplines(start.x, start.y, start.theta, goal.x, goal.y,
            goal.theta, result);
  }
  
  public static boolean reticulateSplines(double x0, double y0, double theta0,
          double x1, double y1, double theta1, Spline result) {
    System.out.println("Reticulating splines...");
    
    // Transform x to the origin
    result.y_offset_ = y0;
    double x1_hat = Math.sqrt((x1-x0)*(x1-x0) + (y1-y0)*(y1-y0));
    if (x1_hat == 0) {
      return false;
    }
    result.x_distance_ = x1_hat;
    result.theta_offset_ = Math.atan2(y1-y0, x1-x0);
    double theta0_hat = theta0 - result.theta_offset_;
    double theta1_hat = theta1 - result.theta_offset_;
    
    // Turn angles into derivatives (slopes)
    if (almostEqual(Math.abs(theta0_hat), Math.PI/2) ||
            almostEqual(Math.abs(theta1_hat), Math.PI/2)) {
      return false;
    }
    if (theta0_hat != 0 && theta1_hat != 0 && 
            Math.signum(theta0_hat) != Math.signum(theta1_hat)) {
      return false;
    }
    double yp0_hat = Math.tan(theta0_hat);
    double yp1_hat = Math.tan(theta1_hat);
    
    // Calculate the cubic spline coefficients
    result.a_ = (3*yp1_hat - yp0_hat) / (x1_hat*x1_hat);
    result.b_ = (yp1_hat - yp0_hat - 3*result.a_*x1_hat*x1_hat) / (2*x1_hat);
    result.c_ = yp0_hat;
    
    return true;
  }
  
  public double calculateLength() {
    final int kNumSamples = 1000;
    double arc_length = 0;
    for (int i = 0; i <= kNumSamples; ++i) {
      double x = ((double)i) / kNumSamples * x_distance_;
      arc_length += Math.sqrt(9*a_*a_*x*x*x*x+12*a_*b_*x*x*x + 6*a_*c_*x*x +
              4*b_*b_*x*x + 4*b_*c_*x + c_*c_+1);
    }
    arc_length /= (kNumSamples + 1);
    arc_length *= x_distance_;
    return arc_length;
  }
  
  public double valueAt(double percentage) {
    percentage = Math.max(Math.min(percentage, 1), 0);
    double x_hat = percentage*x_distance_;
    double y_hat = a_*x_hat*x_hat*x_hat + b_*x_hat*x_hat + c_*x_hat;
    
    double cos_theta = Math.cos(theta_offset_);
    double sin_theta = Math.sin(theta_offset_);
    
    return x_hat * sin_theta + y_hat * cos_theta + y_offset_;
  }
  
  private double slopeAt(double percentage) {
    percentage = Math.max(Math.min(percentage, 1), 0);
    
    double x_hat = percentage*x_distance_;
    double yp_hat = 3*a_*x_hat*x_hat + 2*b_*x_hat + c_;
    
    return yp_hat;
  }
  
  public double angleAt(double percentage) {
    return ChezyMath.boundAngle0to2PiRadians(Math.atan(slopeAt(percentage)) +
            theta_offset_);
  }
  
  public String toString() {
    return "a=" + a_ + "; b=" + b_ + "; c=" + c_;
  }
}
