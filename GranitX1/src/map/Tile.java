package map;

import common.AbstractTask;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Objects;

/**
 *
 * @author kamyshev.a
 */
public class Tile extends AbstractTask {

    /**
     * Global tile number on the x-axis
     */
    public final int nx;

    /**
     * Global tile number on the y-axis
     */
    public final int ny;

    /**
     * Global scale
     */
    public final int z;

    /**
     * Global x-axis zero pixel number
     */
    public final int x0;

    /**
     * Global y-axis zero pixel number
     */
    public final int y0;

    public BufferedImage image;

    public Tile(int nx, int ny, int z) {
        this.nx = nx;
        this.ny = ny;
        this.z = z;
        x0 = this.nx * 256;
        y0 = this.ny * 256;
        CheckTileParameters();
    }

    private void CheckTileParameters() {
        if (nx < 0 || ny < 0) {
            status = StatusEnum.exspired;
            return;
        }
        int max_n = (int) Math.pow(2, z) - 1;
        if (nx > max_n || ny > max_n) {
            status = StatusEnum.exspired;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Tile tile = (Tile) obj;
        return Objects.equals(nx, tile.nx)
                && Objects.equals(ny, tile.ny)
                && Objects.equals(z, tile.z);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + Objects.hashCode(this.nx);
        hash = 37 * hash + Objects.hashCode(this.ny);
        hash = 37 * hash + Objects.hashCode(this.z);
        return hash;
    }

    /**
     * Set image and status "done".
     *
     * @param image
     */
    public void SetImage(BufferedImage image) {
        this.image = image;
        status = StatusEnum.done;
    }

    public String CacheFileName() {
        String fn = File.separator + "map_cache" + File.separator + "yandex_map"
                + File.separator + z + File.separator + nx + File.separator
                + String.format("%010d", nx) + "_"
                + String.format("%010d", ny) + ".png";
        return fn;
    }
}
