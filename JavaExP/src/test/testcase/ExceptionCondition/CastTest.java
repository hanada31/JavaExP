package testcase.ExceptionCondition;

/**
 * @Author hanada
 * @Date 2023/9/7 11:41
 * @Version 1.0
 */
public class CastTest {
    public CastTest(String magicNumber, long offset) {

        if (offset < 0) {
            throw new IllegalArgumentException("The offset cannot be negative");
        }

        if (offset < 5) {
            throw new IllegalArgumentException("offset < 5)");
        }

        if (offset >10) {
            throw new IllegalArgumentException("offset > 10");
        }
    }
    public  boolean testConflict(String path1, String path2) {
        if (path1 == null && path2 == null) {
            return true;
        }
        if (path1 == null || path2 == null) {
            return false;
        }
        if(path1.contains(""))
            throw new RuntimeException("1");
        return true;
    }
}
