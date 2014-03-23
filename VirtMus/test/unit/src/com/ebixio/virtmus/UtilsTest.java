/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ebixio.virtmus;

import java.awt.Cursor;
import java.awt.Dimension;
import java.io.File;
import static org.junit.Assert.*;
import org.junit.*;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class UtilsTest {

    public UtilsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getScreenSize method, of class Utils.
     */
    @Test
    public void testGetScreenSize_0args() {
        System.out.println("getScreenSize");
        Dimension result = Utils.getScreenSize();
        assertNotNull(result);
    }

    /**
     * Test of getScreenSize method, of class Utils.
     */
    @Test
    public void testGetScreenSize_int() {
        System.out.println("getScreenSize");
        int screen = 0;
        Dimension result = Utils.getScreenSize(screen);
        assertNotNull(result);
    }

    /**
     * Test of getScreenSizes method, of class Utils.
     */
    @Test
    public void testGetScreenSizes() {
        System.out.println("getScreenSizes");
        Dimension[] result = Utils.getScreenSizes();
        assertTrue(result.length > 0);
    }

    /**
     * Test of getNumberOfScreens method, of class Utils.
     */
    @Test
    public void testGetNumberOfScreens() {
        System.out.println("getNumberOfScreens");
        int result = Utils.getNumberOfScreens();
        assertTrue(result >= 0);
    }

    /**
     * Test of getInvisibleCursor method, of class Utils.
     */
    @Test
    public void testGetInvisibleCursor() {
        System.out.println("getInvisibleCursor");
        Cursor result = Utils.getInvisibleCursor();
        assertNotNull(result);
    }

//    /**
//     * Test of scaleProportional method, of class Utils.
//     */
//    @Test
//    public void testScaleProportional() {
//        System.out.println("scaleProportional");
//        Rectangle container = null;
//        Rectangle item = null;
//        double expResult = 0.0;
//        double result = Utils.scaleProportional(container, item);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of shrinkToFit method, of class Utils.
//     */
//    @Test
//    public void testShrinkToFit() {
//        System.out.println("shrinkToFit");
//        Rectangle container = null;
//        Rectangle item = null;
//        Rectangle expResult = null;
//        Rectangle result = Utils.shrinkToFit(container, item);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of stretchToFit method, of class Utils.
//     */
//    @Test
//    public void testStretchToFit() {
//        System.out.println("stretchToFit");
//        Rectangle container = null;
//        Rectangle item = null;
//        Rectangle expResult = null;
//        Rectangle result = Utils.stretchToFit(container, item);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of scaleToFit method, of class Utils.
//     */
//    @Test
//    public void testScaleToFit() {
//        System.out.println("scaleToFit");
//        Rectangle container = null;
//        Rectangle item = null;
//        Rectangle expResult = null;
//        Rectangle result = Utils.scaleToFit(container, item);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of centerItem method, of class Utils.
//     */
//    @Test
//    public void testCenterItem() {
//        System.out.println("centerItem");
//        Rectangle container = null;
//        Rectangle item = null;
//        Point expResult = null;
//        Point result = Utils.centerItem(container, item);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of findFileRelative method, of class Utils.
     */
    @Test
    public void testFindFileRelative() {
        System.out.println("findFileRelative");
        File newSrc = null;
        File oldTarget = null;
        File expResult = null;
        File result = Utils.findFileRelative(newSrc, oldTarget);
        assertEquals(expResult, result);
        
        newSrc = new File("C:\\Data\\Music\\Sheet\\Choir\\Choir.playlist.xml");
        oldTarget = new File("D:\\Data\\Music\\Sheet\\Nabucco\\Nabucco.song.xml");
        expResult = new File("C:\\Data\\Music\\Sheet\\Nabucco\\Nabucco.song.xml");
        result = Utils.findFileRelative(newSrc, oldTarget);
        assertEquals(expResult, result);
        
        newSrc = new File("C:\\Data\\Music\\Sheet\\Choir\\Choir.playlist.xml");
        oldTarget = new File("D:\\Data\\Music\\Sheet\\Choir\\Nabucco\\Nabucco.song.xml");
        expResult = new File("C:\\Data\\Music\\Sheet\\Choir\\Nabucco\\Nabucco.song.xml");
        result = Utils.findFileRelative(newSrc, oldTarget);
        assertEquals(expResult, result);
    }

//    /**
//     * Test of openURL method, of class Utils.
//     */
//    @Test
//    public void testOpenURL() {
//        System.out.println("openURL");
//        String url = "";
//        boolean expResult = false;
//        boolean result = Utils.openURL(url);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAppPath method, of class Utils.
//     */
//    @Test
//    public void testGetAppPath() {
//        System.out.println("getAppPath");
//        File expResult = null;
//        File result = Utils.getAppPath();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAppPath1 method, of class Utils.
//     */
//    @Test
//    public void testGetAppPath1() {
//        System.out.println("getAppPath1");
//        File expResult = null;
//        File result = Utils.getAppPath1();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAppPath2 method, of class Utils.
//     */
//    @Test
//    public void testGetAppPath2() {
//        System.out.println("getAppPath2");
//        File expResult = null;
//        File result = Utils.getAppPath2();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of listFilesAsArray method, of class Utils.
//     */
//    @Test
//    public void testListFilesAsArray() {
//        System.out.println("listFilesAsArray");
//        File directory = null;
//        FilenameFilter filter = null;
//        boolean recurse = false;
//        File[] expResult = null;
//        File[] result = Utils.listFilesAsArray(directory, filter, recurse);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of listFiles method, of class Utils.
//     */
//    @Test
//    public void testListFiles() {
//        System.out.println("listFiles");
//        File directory = null;
//        FilenameFilter filter = null;
//        boolean recurse = false;
//        Collection<File> expResult = null;
//        Collection<File> result = Utils.listFiles(directory, filter, recurse);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of shortenString method, of class Utils.
     */
    @Test
    public void testShortenString() {
        System.out.println("shortenString");
        String orig = "12345";
        int charsToRemove = 0;
        String expResult = orig;
        String result = Utils.shortenString(orig, charsToRemove);
        assertEquals(expResult, result);

        assertTrue("12...45".equals(Utils.shortenString(orig, 1)));
        assertTrue("12...5".equals(Utils.shortenString(orig, 2)));
        assertTrue("1...5".equals(Utils.shortenString(orig, 3)));
        assertTrue("1...".equals(Utils.shortenString(orig, 4)));
        assertTrue("1...".equals(Utils.shortenString(orig, 5)));
        assertTrue("1...".equals(Utils.shortenString(orig, 10)));

        orig = "1234";
        assertTrue("12...4".equals(Utils.shortenString(orig, 1)));
        assertTrue("1...4".equals(Utils.shortenString(orig, 2)));
        assertTrue("1...".equals(Utils.shortenString(orig, 3)));
        assertTrue("1...".equals(Utils.shortenString(orig, 4)));
        assertTrue("1234".equals(Utils.shortenString(orig, -10)));
    }

}