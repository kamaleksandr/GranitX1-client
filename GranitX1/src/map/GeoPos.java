
package map;

/**
 *
 * @author kamyshev.a
 */
public class GeoPos {

    public double lon;
    public double lat;

    public GeoPos(double lon, double lat) {
        this.setPosition(lon, lat);
    }

    public void setPosition(GeoPos pos) {
        this.setPosition(pos.lon, pos.lat);
    }

    public final void setPosition(double lon, double lat) {
        this.lon = lon;
        this.lat = lat;
    }
}
