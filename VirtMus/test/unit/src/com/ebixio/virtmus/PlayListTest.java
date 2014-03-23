/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ebixio.virtmus;

import com.ebixio.util.Util;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openide.util.Exceptions;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class PlayListTest {

    public PlayListTest() {
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

//    /**
//     * Test of addAllSongs method, of class PlayList.
//     */
//    @Test
//    public void testAddAllSongs() {
//        System.out.println("addAllSongs");
//        File dir = null;
//        boolean removeExisting = false;
//        PlayList instance = new PlayList();
//        instance.addAllSongs(dir, removeExisting);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of sortSongsByName method, of class PlayList.
//     */
//    @Test
//    public void testSortSongsByName() {
//        System.out.println("sortSongsByName");
//        PlayList instance = new PlayList();
//        instance.sortSongsByName();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of saveAll method, of class PlayList.
//     */
//    @Test
//    public void testSaveAll() {
//        System.out.println("saveAll");
//        PlayList instance = new PlayList();
//        instance.saveAll();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of save method, of class PlayList.
//     */
//    @Test
//    public void testSave() {
//        System.out.println("save");
//        PlayList instance = new PlayList();
//        boolean expResult = false;
//        boolean result = instance.save();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of saveAs method, of class PlayList.
//     */
//    @Test
//    public void testSaveAs() {
//        System.out.println("saveAs");
//        PlayList instance = new PlayList();
//        boolean expResult = false;
//        boolean result = instance.saveAs();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of open method, of class PlayList.
//     */
//    @Test
//    public void testOpen() {
//        System.out.println("open");
//        PlayList expResult = null;
//        PlayList result = PlayList.open();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of serialize method, of class PlayList.
//     */
//    @Test
//    public void testSerialize_0args() {
//        System.out.println("serialize");
//        PlayList instance = new PlayList();
//        boolean expResult = false;
//        boolean result = instance.serialize();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of serialize method, of class PlayList.
     */
    @Test
    public void testSerialize_File() {
        System.out.println("serialize");
        File toFile = null;
        String schema = "/com/ebixio/virtmus/xml/PlayListSchema.xsd";

        try {
            toFile = File.createTempFile("VirtMusJUnit", ".playlist.xml");
            toFile.deleteOnExit();
            System.out.println("serialize to: " + toFile.getAbsolutePath());

            PlayList instance = new PlayList();
            boolean result = instance.serialize(toFile);
            assertEquals(true, result);

            // Default (empty) playlist
            InputStream xsd = Song.class.getResourceAsStream(schema);
            assertEquals(true, Util.validateXml(toFile, xsd));

            // Adding name (but no songs)
            instance.setName("Some playlist name");
            instance.serialize(toFile);
            xsd = Song.class.getResourceAsStream(schema);   // xsd.reset() not supported
            assertEquals(true, Util.validateXml(toFile, xsd));

            Song song = new Song();
            song.setSourceFile(new File("SomeFile.song.xml"));

            // With 1 song
            instance.addSong(song);
            instance.serialize(toFile);
            xsd = Song.class.getResourceAsStream(schema);
            assertEquals(true, Util.validateXml(toFile, xsd));

            // With 2 songs
            instance.addSong(song);
            instance.serialize(toFile);
            xsd = Song.class.getResourceAsStream(schema);
            assertEquals(true, Util.validateXml(toFile, xsd));

            // With no name
            instance.setName(null);
            instance.serialize(toFile);
            xsd = Song.class.getResourceAsStream(schema);
            assertEquals(true, Util.validateXml(toFile, xsd));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {

        }
    }

//    /**
//     * Test of deserialize method, of class PlayList.
//     */
//    @Test
//    public void testDeserialize() {
//        System.out.println("deserialize");
//        File f = null;
//        PlayList expResult = null;
//        PlayList result = PlayList.deserialize(f);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addSong method, of class PlayList.
//     */
//    @Test
//    public void testAddSong() {
//        System.out.println("addSong");
//        Song song = null;
//        PlayList instance = new PlayList();
//        instance.addSong(song);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of removeSong method, of class PlayList.
//     */
//    @Test
//    public void testRemoveSong() {
//        System.out.println("removeSong");
//        Song song = null;
//        PlayList instance = new PlayList();
//        boolean expResult = false;
//        boolean result = instance.removeSong(song);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of reorder method, of class PlayList.
//     */
//    @Test
//    public void testReorder() {
//        System.out.println("reorder");
//        int[] order = null;
//        PlayList instance = new PlayList();
//        instance.reorder(order);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getSourceFile method, of class PlayList.
//     */
//    @Test
//    public void testGetSourceFile() {
//        System.out.println("getSourceFile");
//        PlayList instance = new PlayList();
//        File expResult = null;
//        File result = instance.getSourceFile();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setSourceFile method, of class PlayList.
//     */
//    @Test
//    public void testSetSourceFile() {
//        System.out.println("setSourceFile");
//        File sourceFile = null;
//        PlayList instance = new PlayList();
//        instance.setSourceFile(sourceFile);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getName method, of class PlayList.
//     */
//    @Test
//    public void testGetName() {
//        System.out.println("getName");
//        PlayList instance = new PlayList();
//        String expResult = "";
//        String result = instance.getName();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setName method, of class PlayList.
//     */
//    @Test
//    public void testSetName() {
//        System.out.println("setName");
//        String name = "";
//        PlayList instance = new PlayList();
//        instance.setName(name);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of isDirty method, of class PlayList.
//     */
//    @Test
//    public void testIsDirty() {
//        System.out.println("isDirty");
//        PlayList instance = new PlayList();
//        boolean expResult = false;
//        boolean result = instance.isDirty();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setDirty method, of class PlayList.
//     */
//    @Test
//    public void testSetDirty() {
//        System.out.println("setDirty");
//        boolean isDirty = false;
//        PlayList instance = new PlayList();
//        instance.setDirty(isDirty);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addChangeListener method, of class PlayList.
//     */
//    @Test
//    public void testAddChangeListener() {
//        System.out.println("addChangeListener");
//        ChangeListener listener = null;
//        PlayList instance = new PlayList();
//        instance.addChangeListener(listener);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of removeChangeListener method, of class PlayList.
//     */
//    @Test
//    public void testRemoveChangeListener() {
//        System.out.println("removeChangeListener");
//        ChangeListener listener = null;
//        PlayList instance = new PlayList();
//        instance.removeChangeListener(listener);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of notifyListeners method, of class PlayList.
//     */
//    @Test
//    public void testNotifyListeners() {
//        System.out.println("notifyListeners");
//        PlayList instance = new PlayList();
//        instance.notifyListeners();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of compareTo method, of class PlayList.
//     */
//    @Test
//    public void testCompareTo() {
//        System.out.println("compareTo");
//        PlayList other = null;
//        PlayList instance = new PlayList();
//        int expResult = 0;
//        int result = instance.compareTo(other);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

}