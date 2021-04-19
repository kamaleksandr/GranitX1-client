package map;

import common.AbstractChannel;
import common.AbstractClient;
import common.AbstractTask;
import common.HTTPChannel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Objects;
import javax.imageio.ImageIO;

/**
 *
 * @author kamyshev.a
 */
public class YandexMap extends AbstractClient {

    private static final int LOAD_TIMEOUT = 10;
    private static final int READ_TIMEOUT = 5;
    private static final int CHANNEL_COUNT = 2;
    private static final double C1 = 0.00335655146887969;
    private static final double C2 = 0.00000657187271079536;
    private static final double C3 = 0.00000001764564338702;
    private static final double C4 = 0.00000000005328478445;
    private final String def_dir_path;
    private final Point point;
    private final Point center;
    private final Rectangle rectangle;
    private int scale, width, height;
    private CreateTilesThread thread;

    public SocketAddress proxy_server;
    public String proxy_authorization;
    public BufferedImage image;

    public YandexMap(GeoPos pos, int scale) throws Exception {
        relevance_timeout = LOAD_TIMEOUT;
        read_timeout = READ_TIMEOUT;
        def_dir_path = new File("").getAbsolutePath();
        point = new Point();
        rectangle = new Rectangle(0, 0, 256, 256);
        center = new Point(128, 128);
        image = new BufferedImage(rectangle.width,
                rectangle.height, BufferedImage.TYPE_INT_ARGB);
        while (channels.size() < CHANNEL_COUNT) {
            channels.add(null);
        }
        setScale(scale);
        point.x = LonToX(pos.lon);
        point.y = LatToY(pos.lat);
        thread = new CreateTilesThread();
    }

    /**
     * @return the rectangle
     */
    public Rectangle getRectangle() {
        return rectangle;
    }

    /**
     * @param rectangle the rectangle to set
     */
    public void setRectangle(Rectangle rectangle) {
        if (rectangle.width < 256) {
            rectangle.width = 256;
        }
        if (rectangle.height < 256) {
            rectangle.height = 256;
        }
        center.x = rectangle.width / 2;
        center.y = rectangle.height / 2;
        if (rectangle.width == 0 || rectangle.height == 0) {
            return;
        }
        this.rectangle.setBounds(rectangle);
        try {
            image = new BufferedImage(rectangle.width,
                    rectangle.height, BufferedImage.TYPE_INT_ARGB);
        } catch (Exception ex) {
            Log(ex);
        }
        GetMap(new Point(0, 0));
    }

    /**
     * @return the scale
     */
    public int getScale() {
        return scale;
    }

    /**
     * @param scale the scale to set
     */
    private void setScale(int scale) {
        if (scale >= 0 && scale <= 17) {
            this.scale = scale;
            width = (int) (256 * Math.pow(2, scale));
            height = width;
        }
    }

    /**
     * @return Global point number in pixels of the current local map center
     */
    public Point getPoint() {
        return point;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    private void ToCache(Tile tile) {
        String fn = def_dir_path + tile.CacheFileName();
        File f = new File(fn);
        if (!f.exists()) {
            f.mkdirs();
            try {
                ImageIO.write(tile.image, "png", f);
            } catch (IOException ex) {
                Log(ex);
            }
        }
    }

    private boolean FromCache(Tile tile) {
        String fn = def_dir_path + tile.CacheFileName();
        File f = new File(fn);
        if (f.exists()) {
            try {
                tile.SetImage(ImageIO.read(f));
            } catch (IOException ex) {
                Log(ex);
            }
        }
        return tile.image != null;
    }

    /**
     *
     * @param task
     * @return
     */
    @Override
    protected ByteBuffer getRequestData(AbstractTask task) {
        Tile tile = (Tile) task;
        String req = "GET http://vec01.maps.yandex.net/tiles?l=map&v=2.30.0&y="
                + tile.ny + "&x=" + tile.nx + "&z=" + tile.z + " HTTP/1.1\r\n"
                + "Host: vec01.maps.yandex.net\r\n";
        if (proxy_server != null) {
            req += "Proxy-Connection: keep-alive\r\nProxy-Authorization: "
                    + proxy_authorization + "\r\n";
        }
        req += "\r\n";
        ByteBuffer buf = ByteBuffer.allocate(req.length());
        buf.put(req.getBytes(StandardCharsets.UTF_8));
        buf.flip();
        return buf;
    }

    @Override
    protected final void OnChannelPacket(Object object) {
        synchronized (lock) {
            HTTPChannel channel = (HTTPChannel) object;
            if (channel.task == null) {
                return;
            }
            Tile tile = (Tile) channel.task;
            channel.task = null;
            if (channel.getContentLength() > 0) {
                ByteArrayInputStream bais = new ByteArrayInputStream(
                        channel.getInputBuffer().array(),
                        channel.getContentPosition(),
                        channel.getContentLength());
                try {
                    tile.SetImage(ImageIO.read(bais));
                } catch (IOException ex) {
                    Log(ex);
                }
            }
            if (tile.image == null) {
                return;
            }
            ToCache(tile);
            if (tile.z == scale) {
                DrawImage(tile);
                Data(tile);
            }
            SetChannelTask(channel);
        }
    }

    @Override
    protected AbstractChannel CreateChannel() {
        try {
            if (proxy_server == null) {
                return new HTTPChannel(new InetSocketAddress("vec01.maps.yandex.net", 80));
            } else {
                return new HTTPChannel(proxy_server);
            }
        } catch (Exception ex) {
            Log(ex);
            return null;
        }
    }

    private void RunCreateTiles() {
        thread.Cancel();
        thread = new CreateTilesThread();
        thread.start();
    }

    class CreateTilesThread extends Thread {

        private volatile boolean canceled = false;

        public void Cancel() {
            this.canceled = true;
        }

        @Override
        public void run() {
            synchronized (lock) {
                LinkedList<Tile> tiles_cache = new LinkedList<>();
                tasks.forEach((task) -> {
                    if (task.getStatus() == Tile.StatusEnum.done) {
                        tiles_cache.add((Tile) task);
                    }
                });
                tasks.clear();

                if (canceled) {
                    return;
                }

                // center tile
                Tile ct = new Tile(point.x / 256, point.y / 256, scale);
                tasks.add(ct);

                // distance in pixels from the center tile to the border of the rectangle
                int a = center.x - point.x + ct.x0;
                int b = center.y - point.y + ct.y0;
                int c = rectangle.width - a - 256;
                int d = rectangle.height - b - 256;

                // number of tiles fully included in this distance
                int na = Math.abs(a) / 256 + 1;
                int nb = Math.abs(b) / 256 + 1;
                int nc = Math.abs(c) / 256 + 1;
                int nd = Math.abs(d) / 256 + 1;

                int nx_max = ct.nx + nc;
                int ny_max = ct.ny + nd;

                for (int nx = ct.nx - na; nx <= nx_max; nx++) {
                    for (int ny = ct.ny - nb; ny <= ny_max; ny++) {

                        if (canceled) {
                            return;
                        }

                        if (nx != ct.nx || ny != ct.ny) {
                            Tile t = new Tile(nx, ny, scale);
                            if (t.getStatus() == Tile.StatusEnum.prepared) {
                                tasks.add(t);
                            }
                        }
                    }
                }

                Graphics2D graphics = (Graphics2D) image.getGraphics();
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

                boolean call_on_data = false;

                ListIterator<AbstractTask> it1 = tasks.listIterator();
                while (it1.hasNext()) {
                    Tile tile1 = (Tile) it1.next();
                    ListIterator<Tile> it2 = tiles_cache.listIterator();
                    while (it2.hasNext()) {
                        Tile tile2 = it2.next();

                        if (canceled) {
                            return;
                        }

                        if (tile2.getStatus() == Tile.StatusEnum.done
                                && Objects.equals(tile1, tile2)) {
                            tile1.SetImage(tile2.image);
                            DrawImage(tile1);
                            call_on_data = true;
                        }

                    }
                }
                ListIterator<AbstractTask> it = tasks.listIterator();
                while (it.hasNext()) {
                    Tile tile = (Tile) it.next();
                    if (tile.getStatus() == Tile.StatusEnum.prepared) {

                        if (canceled) {
                            return;
                        }

                        if (FromCache(tile)) {
                            call_on_data = true;
                            DrawImage(tile);
                        }
                    }
                }

                if (canceled) {
                    return;
                }

                if (call_on_data) {
                    Data(ct);
                }
            }
            Maintenance();
        }

    }

    /**
     * Set position, scale and start loading map.
     *
     * @param pos Geographic position in degrees
     * @param scale Global scale (0-17)
     */
    public void GetMap(GeoPos pos, int scale) {
        setScale(scale);
        point.x = LonToX(pos.lon);
        point.y = LatToY(pos.lat);
        RunCreateTiles();
    }

    /**
     * Apply shift of position, start loading map.
     *
     * @param shift The shift in pixels
     */
    public void GetMap(Point shift) {
        point.move(point.x - shift.x, point.y - shift.y);
        RunCreateTiles();
    }

    /**
     * Apply shift of position, set scale and start loading map.
     *
     * @param shift The shift in pixels
     * @param scale Global scale (0-17)
     */
    public void GetMap(Point shift, int scale) {
        point.move(point.x - shift.x, point.y - shift.y);
        GeoPos pos = new GeoPos(XToLon(point.x), YToLat(point.y));
        setScale(scale);
        point.x = LonToX(pos.lon);
        point.y = LatToY(pos.lat);
        RunCreateTiles();
    }

    private void DrawImage(Tile tile) {
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        int x = center.x - point.x + tile.x0;
        int y = center.y - point.y + tile.y0;
        graphics.drawImage(tile.image, x, y, null);
        //graphics.setColor(Color.black);
        //graphics.drawRect(x, y, 256, 256);
    }

    /**
     * Translation of pixel X-axis at longitude.
     *
     * @param x Pixel X-axis
     * @return Longitude
     */
    public final double XToLon(int x) {
        double lon = 360.0 * x / (256 * Math.pow(2, scale)) - 180.0;
        if (lon > 180) {
            lon -= 360;
        }
        return (lon);
    }

    /**
     * Translations longitude in coordinates of pixel X-axis.
     *
     * @param lon Longitude
     * @return Pixel X-axis
     */
    public final int LonToX(double lon) {
        return (int) Math.round((lon + 180.0) / 360.0 * (256 * Math.pow(2, scale)));
    }

    /**
     * Translation of pixel Y-axis at latitude.
     *
     * @param y Pixel Y-axis
     * @return Latitude
     */
    public final double YToLat(int y) {
        double mercY = 20037508.342789 - (y * Math.pow(2, 23 - scale)) / 53.5865938;
        double g = Math.PI / 2 - 2 * Math.atan(1 / Math.exp(mercY / 6378137.0));
        double zz = g + C1 * Math.sin(2 * g) + C2 * Math.sin(4 * g)
                + C3 * Math.sin(6 * g) + C4 * Math.sin(8 * g);
        return zz * 180 / Math.PI;
    }

    /**
     * Translations latitude in coordinates of pixel Y-axis.
     *
     * @param lat Latitude
     * @return Pixel Y-axis
     */
    public final int LatToY(double lat) {
        double rLat = lat * Math.PI / 180;
        double a = 6378137.0;
        double k = 0.0818191908426;
        double zz = Math.tan(Math.PI / 4 + rLat / 2)
                / Math.pow((Math.tan(Math.PI / 4 + Math.asin(k * Math.sin(rLat)) / 2)), k);
        double y = (20037508.342789 - a * Math.log(zz)) * 53.5865938 / Math.pow(2, 23 - scale);
        return (int) Math.round(y);
    }
}
