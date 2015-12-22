package core;
import java.math.BigInteger;


public class LeastCommonMultiple
{
  public static int compute(int... nums)
  {
    if (false) { 
      System.out.print("LeastCommonMultiple.compute(");
      for (int i = 0; i < nums.length; i++)
        System.out.print(nums[i]+",");
      System.out.println(");");
    }

    // Recursively iterate through pairs of arguments
    // i.e. lcm(args[0], lcm(args[1], lcm(args[2], args[3])))
    if (nums.length == 2)
      return leastCommonMultiple(nums[0], nums[1]);
    return leastCommonMultiple(nums[0], compute(shift(nums)));
  }

  private static int[] shift(int... nums)
  {
    int[] remaining = new int[nums.length - 1];
    for (int i = 0; i < remaining.length; i++)
      remaining[i] = nums[i + 1];
    return remaining;
  }
  
  private static int greatestCommonDivisor(int a, int b)
  {
    int t;  // euclidean alg
    while (b != 0) {
      t = b;
      b = a % b;
      a = t;
    }
    return a;
  }

  private static int leastCommonMultiple(int a, int b)
  {
    int gcd = greatestCommonDivisor(a, b);
    BigInteger bGCD = BigInteger.valueOf(gcd);
    BigInteger bA = BigInteger.valueOf(a);
    BigInteger bB = BigInteger.valueOf(b);
    BigInteger bAB = bA.multiply(bB);
    return bAB.divide(bGCD).intValue();
  }

  public static void main(String[] args)
  {
   // System.out.println(leastCommonMultiple(70560, 35280));
//    System.out.println(compute(2,3,7));
    System.out.println(compute(new int[]{141120, 141120,70560, 35280}));
  }
}
