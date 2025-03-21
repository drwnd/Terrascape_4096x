package terrascape.entity;

import terrascape.server.Material;
import terrascape.dataStorage.octree.Chunk;
import terrascape.utils.Utils;
import org.joml.Vector3f;
import org.joml.Vector3i;

import static terrascape.utils.Constants.*;
import static terrascape.utils.Settings.*;

public record Target(Vector3i position, int side, byte material) {

    public static Target getTarget(Vector3f origin, Vector3f dir) {

        int x = Utils.floor(origin.x);
        int y = Utils.floor(origin.y);
        int z = Utils.floor(origin.z);

        int xDir = dir.x < 0 ? -1 : 1;
        int yDir = dir.y < 0 ? -1 : 1;
        int zDir = dir.z < 0 ? -1 : 1;

        int xSide = dir.x < 0 ? WEST : EAST;
        int ySide = dir.y < 0 ? TOP : BOTTOM;
        int zSide = dir.z < 0 ? NORTH : SOUTH;

        double dirXSquared = dir.x * dir.x;
        double dirYSquared = dir.y * dir.y;
        double dirZSquared = dir.z * dir.z;
        double xUnit = (float) Math.sqrt(1 + (dirYSquared + dirZSquared) / dirXSquared);
        double yUnit = (float) Math.sqrt(1 + (dirXSquared + dirZSquared) / dirYSquared);
        double zUnit = (float) Math.sqrt(1 + (dirXSquared + dirYSquared) / dirZSquared);

        double lengthX = xUnit * (dir.x < 0 ? Utils.fraction(origin.x) : 1 - Utils.fraction(origin.x));
        double lengthY = yUnit * (dir.y < 0 ? Utils.fraction(origin.y) : 1 - Utils.fraction(origin.y));
        double lengthZ = zUnit * (dir.z < 0 ? Utils.fraction(origin.z) : 1 - Utils.fraction(origin.z));
        double length = 0;

        int intersectedSide = 0;
        while (length < REACH) {

            byte material = Chunk.getMaterialInWorld(x, y, z);
            if (material == OUT_OF_WORLD) return null;

            if ((Material.getMaterialProperties(material) & NO_COLLISION) == 0) {
               return new Target(new Vector3i(x, y, z), intersectedSide, material);
            }

            if (lengthX < lengthZ && lengthX < lengthY) {
                x += xDir;
                length = lengthX;
                lengthX += xUnit;
                intersectedSide = xSide;
            } else if (lengthZ < lengthX && lengthZ < lengthY) {
                z += zDir;
                length = lengthZ;
                lengthZ += zUnit;
                intersectedSide = zSide;
            } else {
                y += yDir;
                length = lengthY;
                lengthY += yUnit;
                intersectedSide = ySide;
            }
        }
        return null;
    }
}
