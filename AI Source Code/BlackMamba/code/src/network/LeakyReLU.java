package network;

public class LeakyReLU extends ActivationFunction{
    public double func(double outputs){
        if(outputs >= 0)
            return outputs;
        else
            return 0.01 * outputs;
    }
}
