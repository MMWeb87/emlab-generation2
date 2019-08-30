package emlab.gen.engine;

/**
 * Agent interface. Every agent has to implement this.
 * @author alfredas
 *
 */
public interface Agent {
    
    public String getName();
    public void setName(String name);
    public void act(Role<? extends Agent> role);

}
