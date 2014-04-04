/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ebixio.virtmus;

import com.ebixio.util.Util;
import com.ebixio.virtmus.MainApp.Rotation;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.event.ChangeListener;
import javax.xml.validation.Validator;
import org.apache.fop.pdf.PDFFileSpec;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openide.util.Exceptions;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class SongTest {
    static Validator xsdValidator = null;
    
    public SongTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        String schema = "/com/ebixio/virtmus/xml/SongSchema.xsd";
        InputStream xsd = Song.class.getResourceAsStream(schema);
        xsdValidator = Util.getValidator(xsd);
        if (xsdValidator == null) {
            throw new Exception("XSD Validator creation failed");
        }
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
     * Test of isDirty method, of class Song.
     */
    @Test
    public void testIsDirty() {
        System.out.println("isDirty");
        Song instance = new Song();
        boolean expResult = false;
        boolean result = instance.isDirty();
        assertEquals(expResult, result);

        instance.setDirty(true);
        expResult = true;
        result = instance.isDirty();
        assertEquals(expResult, result);

        instance.setName("New name");
        assertEquals(true, instance.isDirty());
    }

    /**
     * Test of setDirty method, of class Song.
     */
    @Test
    public void testSetDirty() {
        System.out.println("setDirty");
        Song instance = new Song();

        instance.setDirty(true);
        assertEquals(true, instance.isDirty());

        instance.setDirty(false);
        assertEquals(false, instance.isDirty());
    }

//    /**
//     * Test of addPage method, of class Song.
//     */
//    @Test
//    public void testAddPage_0args() {
//        System.out.println("addPage");
//        Song instance = new Song();
//        boolean expResult = false;
//        boolean result = instance.addPage();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addPage method, of class Song.
//     */
//    @Test
//    public void testAddPage_File() {
//        System.out.println("addPage");
//        File f = null;
//        Song instance = new Song();
//        boolean expResult = false;
//        boolean result = instance.addPage(f);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addPage method, of class Song.
//     */
//    @Test
//    public void testAddPage_MusicPage() {
//        System.out.println("addPage");
//        MusicPage mp = null;
//        Song instance = new Song();
//        boolean expResult = false;
//        boolean result = instance.addPage(mp);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addPage method, of class Song.
//     */
//    @Test
//    public void testAddPage_MusicPage_int() {
//        System.out.println("addPage");
//        MusicPage mp = null;
//        int index = 0;
//        Song instance = new Song();
//        boolean expResult = false;
//        boolean result = instance.addPage(mp, index);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of removePage method, of class Song.
//     */
//    @Test
//    public void testRemovePage() {
//        System.out.println("removePage");
//        MusicPage[] mps = null;
//        Song instance = new Song();
//        boolean expResult = false;
//        boolean result = instance.removePage(mps);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of reorder method, of class Song.
//     */
//    @Test
//    public void testReorder() {
//        System.out.println("reorder");
//        int[] order = null;
//        Song instance = new Song();
//        instance.reorder(order);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getSourceFile method, of class Song.
//     */
//    @Test
//    public void testGetSourceFile() {
//        System.out.println("getSourceFile");
//        Song instance = new Song();
//        File expResult = null;
//        File result = instance.getSourceFile();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setSourceFile method, of class Song.
//     */
//    @Test
//    public void testSetSourceFile() {
//        System.out.println("setSourceFile");
//        File sourceFile = null;
//        Song instance = new Song();
//        instance.setSourceFile(sourceFile);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setName method, of class Song.
//     */
//    @Test
//    public void testSetName() {
//        System.out.println("setName");
//        String name = "";
//        Song instance = new Song();
//        instance.setName(name);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getName method, of class Song.
//     */
//    @Test
//    public void testGetName() {
//        System.out.println("getName");
//        Song instance = new Song();
//        String expResult = "";
//        String result = instance.getName();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of save method, of class Song.
//     */
//    @Test
//    public void testSave() {
//        System.out.println("save");
//        Song instance = new Song();
//        boolean expResult = false;
//        boolean result = instance.save();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of saveAs method, of class Song.
//     */
//    @Test
//    public void testSaveAs() {
//        System.out.println("saveAs");
//        Song instance = new Song();
//        boolean expResult = false;
//        boolean result = instance.saveAs();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of open method, of class Song.
//     */
//    @Test
//    public void testOpen() {
//        System.out.println("open");
//        Song expResult = null;
//        Song result = Song.open();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of serialize method, of class Song.
//     */
//    @Test
//    public void testSerialize_0args() {
//        System.out.println("serialize");
//        Song instance = new Song();
//        boolean expResult = false;
//        boolean result = instance.serialize();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }


    /**
     * Test of serialize method, of class Song.
     */
    @Test
    public void testSerialize_File() {
        File toFile = null;

        try {
            toFile = File.createTempFile("VirtMusJUnit", ".song.xml");
            toFile.deleteOnExit();
            System.out.println("serialize to: " + toFile.getAbsolutePath());

            Song instance = new Song();
            instance.setSourceFile(toFile);
            assertEquals(true, instance.serialize(toFile));
            assertEquals(true, Util.validateXml(toFile, xsdValidator));

            instance.setName("Some song name");
            assertEquals(true, instance.serialize(toFile));
            assertEquals(true, Util.validateXml(toFile, xsdValidator));

            instance.addPage(toFile);
            assertEquals(true, instance.serialize(toFile));
            assertEquals(true, Util.validateXml(toFile, xsdValidator));

            File pdfFile = new File("SomePdfFile.pdf");
            instance.addPage(new MusicPageSVG(instance, pdfFile, 3));
            instance.addPage(new MusicPageSVG(instance, pdfFile, 24));
            assertEquals(true, instance.serialize(toFile));
            assertEquals(true, Util.validateXml(toFile, xsdValidator));

            instance.setName(null);
            assertEquals(true, instance.serialize(toFile));
            assertEquals(true, Util.validateXml(toFile, xsdValidator));

            instance.pageOrder.get(0).rotation = Rotation.Clockwise_90;
            assertEquals(true, instance.serialize(toFile));
            assertEquals(true, Util.validateXml(toFile, xsdValidator));

            instance.addPage(instance.pageOrder.get(0).clone());
            instance.addPage(instance.pageOrder.get(0).clone());
            assertEquals(true, instance.serialize(toFile));
            assertEquals(true, Util.validateXml(toFile, xsdValidator));
            
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (toFile != null) {
                toFile.delete();
            }
        }

    }

//    /**
//     * Test of deserialize method, of class Song.
//     */
//    @Test
//    public void testDeserialize() {
//        System.out.println("deserialize");
//        File f = null;
//        Song expResult = null;
//        Song result = Song.deserialize(f);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of findPages method, of class Song.
//     */
//    @Test
//    public void testFindPages() {
//        System.out.println("findPages");
//        Song s = null;
//        Song.findPages(s);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of clearInstantiated method, of class Song.
//     */
//    @Test
//    public void testClearInstantiated() {
//        System.out.println("clearInstantiated");
//        Song.clearInstantiated();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addPropertyChangeListener method, of class Song.
//     */
//    @Test
//    public void testAddPropertyChangeListener() {
//        System.out.println("addPropertyChangeListener");
//        PropertyChangeListener pcl = null;
//        Song instance = new Song();
//        instance.addPropertyChangeListener(pcl);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of removePropertyChangeListener method, of class Song.
//     */
//    @Test
//    public void testRemovePropertyChangeListener() {
//        System.out.println("removePropertyChangeListener");
//        PropertyChangeListener pcl = null;
//        Song instance = new Song();
//        instance.removePropertyChangeListener(pcl);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addChangeListener method, of class Song.
//     */
//    @Test
//    public void testAddChangeListener() {
//        System.out.println("addChangeListener");
//        ChangeListener listener = null;
//        Song instance = new Song();
//        instance.addChangeListener(listener);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of removeChangeListener method, of class Song.
//     */
//    @Test
//    public void testRemoveChangeListener() {
//        System.out.println("removeChangeListener");
//        ChangeListener listener = null;
//        Song instance = new Song();
//        instance.removeChangeListener(listener);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of notifyListeners method, of class Song.
//     */
//    @Test
//    public void testNotifyListeners() {
//        System.out.println("notifyListeners");
//        Song instance = new Song();
//        instance.notifyListeners();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of compareTo method, of class Song.
//     */
//    @Test
//    public void testCompareTo() {
//        System.out.println("compareTo");
//        Song other = null;
//        Song instance = new Song();
//        int expResult = 0;
//        int result = instance.compareTo(other);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

}