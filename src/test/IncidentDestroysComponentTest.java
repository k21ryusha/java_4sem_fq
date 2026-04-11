package test;

import components.Car;
import components.Component;
import incidents.IncidentService;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IncidentDestroysComponentTest {
    @Test
    public void mechanicalFailureDestroysComponentWhenChanceTriggers() {
        Car car = TestSupport.createCar();
        car.getEngine().setWear(70);
        car.getTransmission().setWear(30);

        IncidentService service = new IncidentService(new TestSupport.PredictableRandom(new double[]{0.0}, new int[]{0}));

        Component broken = service.checkMechanicalFailure(car);

        assertNotNull(broken);
        assertTrue(car.getEngine().isDestroyed());
    }
}
