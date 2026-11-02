package borrowedenergy.model;

public record Particle(double x, double y, double vx, double vy, double life, double maxLife) {
    Particle advance(double dt) { return new Particle(x + vx * dt, y + vy * dt, vx, vy, life - dt, maxLife); }
}
