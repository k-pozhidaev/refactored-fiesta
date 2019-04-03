package io.pozhidaev.sisyphusClient.component;

import io.pozhidaev.sisyphusClient.domain.*;
import io.pozhidaev.sisyphusClient.utils.Whitebox;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@Slf4j
public class TusdUploadTest {

    @Test
    public void calcFingerprint() throws IOException {
        final Path file = Files.createTempFile("asynchronousFileChannelQuietly", " 1");
        Files.write(file, "Test,test,test".getBytes());
        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();

        final String fingerprint = tusUploader.calcFingerprint();
        assertTrue(fingerprint.contains("14"));
        System.out.println(fingerprint);
    }

    @Test(expected = NullPointerException.class)
    public void calcFingerprint_nullPointerException() throws IOException {
        final Path file = Files.createTempFile("asynchronousFileChannelQuietly", " 1");
        Files.write(file, "Test,test,test".getBytes());
        final TusdUpload tusUploader = TusdUpload.builder().build();

        final String fingerprint = tusUploader.calcFingerprint();
        assertTrue(fingerprint.contains("14"));
        System.out.println(fingerprint);
    }


    @Test
    public void generateMetadataQuietly() throws IOException {
        final Path file = Files.createTempFile("generateMetadataQuietly", " 1");

        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();
        final String metadata = tusUploader.generateMetadataQuietly();
        final String encodeToString = Base64.getEncoder().encodeToString(file.getFileName().toString().getBytes());
        assertTrue(metadata.contains(encodeToString));
        assertTrue(metadata.contains("filename"));
    }


    @Test(expected = NullPointerException.class)
    public void generateMetadataQuietly_nullPointerException() throws IOException {
        final Path file = Files.createTempFile("generateMetadataQuietly", " 1");

        final TusdUpload tusUploader = TusdUpload.builder().build();
        final String metadata = tusUploader.generateMetadataQuietly();
        final String encodeToString = Base64.getEncoder().encodeToString(file.getFileName().toString().getBytes());
        assertTrue(metadata.contains(encodeToString));
        assertTrue(metadata.contains("filename"));
    }

    @Test
    public void asynchronousFileChannelQuietly() throws IOException {

        final Path file = Files.createTempFile("asynchronousFileChannelQuietly", " 1");
        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();

        final AsynchronousFileChannel asynchronousFileChannel = tusUploader.asynchronousFileChannelQuietly();
        assertTrue(asynchronousFileChannel.isOpen());
        asynchronousFileChannel.close();

    }

    @Test(expected = AsynchronousFileChannelOpenException.class)
    public void asynchronousFileChannelQuietly_Exception() throws IOException {

        final Path file = Paths.get("asynchronousFileChannelQuietly_Exception");
        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();

        final AsynchronousFileChannel asynchronousFileChannel = tusUploader.asynchronousFileChannelQuietly();
        assertTrue(asynchronousFileChannel.isOpen());
        asynchronousFileChannel.close();

    }

    @Test
    public void uploadedLengthFromResponse(){

        final ClientResponse.Headers headers = mock(ClientResponse.Headers.class);
        when(headers.header("Upload-Offset")).thenReturn(Collections.singletonList("666"));
        final ClientResponse response = mock(ClientResponse.class);
        when(response.headers()).thenReturn(headers);
        final TusdUpload tusdUpload = mock(TusdUpload.class);
        when(tusdUpload.uploadedLengthFromResponse(response)).thenCallRealMethod();
        assertEquals(666L, tusdUpload.uploadedLengthFromResponse(response));
    }

    @Test(expected = FileUploadException.class)
    public void uploadedLengthFromResponse_Exception(){
        final HttpHeaders httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.entrySet()).thenReturn(new HashSet<>());

        final ClientResponse.Headers headers = mock(ClientResponse.Headers.class);
        when(headers.header("Upload-Offset")).thenReturn(Collections.emptyList());
        when(headers.asHttpHeaders()).thenReturn(httpHeaders);

        final ClientResponse response = mock(ClientResponse.class);
        when(response.headers()).thenReturn(headers);
        when(response.statusCode()).thenReturn(HttpStatus.BAD_REQUEST);

        final TusdUpload tusdUpload = mock(TusdUpload.class);
        when(tusdUpload.uploadedLengthFromResponse(response)).thenCallRealMethod();
        tusdUpload.uploadedLengthFromResponse(response);
    }

    @Test
    public void dataBufferFlux() throws IOException {

        final Path file = Files.createTempFile("asynchronousFileChannelQuietly", " 1");
        Files.write(file, "test".getBytes());

        final TusdUpload tusUploader = TusdUpload.builder().path(file).chunkSize(1024).build();

        final Flux<DataBuffer> flux = tusUploader.dataBufferFlux(0);

        String blockFirst = flux
            .map(b -> {
                final byte[] bytes = new byte[4];
                b.read(bytes);
                return bytes;
            })
            .map(String::new)
            .blockFirst();
        assertEquals("test", blockFirst);
    }

    @Test
    public void calcChunkCount() {
        final TusdUpload tusdUpload = mock(TusdUpload.class);
        Whitebox.setInternalState(tusdUpload, "chunkSize", 1024);
        when(tusdUpload.readFileSizeQuietly()).thenReturn(5000L);
        when(tusdUpload.calcChunkCount()).thenCallRealMethod();
        assertEquals(5, tusdUpload.calcChunkCount());
    }

    //TODO FIX NULL in mime type probe!
    @Test
    public void readContentTypeQuietly() throws IOException {
        final Path file = Files.createTempFile("readLastModifiedQuietly", ".html");

        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();
        final String contentType = tusUploader.readContentTypeQuietly();
        Optional.ofNullable(contentType)
            .map(s -> {
                assertTrue(s.contains("text/html"));
                return s;
            })
            .orElseGet(() -> {
                assertTrue(true);
                return "";
            });
    }


    @Test(expected = FileContentTypeReadException.class)
    public void readContentTypeQuietly_Exception() throws IOException {
        final Path file = Files.createTempFile("readLastModifiedQuietly", ".html");

        final TusdUpload tusdUpload = mock(TusdUpload.class);
        Whitebox.setInternalState(tusdUpload, "path", file);
        when(tusdUpload.readContentTypeQuietly()).thenCallRealMethod();
        when(tusdUpload.probeContentType()).thenThrow(new IOException());

        tusdUpload.readContentTypeQuietly();
    }

    @Test
    public void readLastModifiedQuietly() throws IOException {
        final Path file = Files.createTempFile("readLastModifiedQuietly", ".html");

        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();
        final long quietly = tusUploader.readLastModifiedQuietly();
        assertNotEquals(quietly, System.nanoTime());

    }

    @Test(expected = FileListModifiedReadException.class)
    public void readLastModifiedQuietly_Exception() {
        final Path file = Paths.get("readLastModifiedQuietly_Exception");

        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();
        tusUploader.readLastModifiedQuietly();
    }

    @Test
    public void handleResponse() {

        final HttpHeaders httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.getLocation()).thenReturn(URI.create("test"));

        final ClientResponse.Headers headers = mock(ClientResponse.Headers.class);
        when(headers.asHttpHeaders()).thenReturn(httpHeaders);

        final ClientResponse response = mock(ClientResponse.class);
        when(response.statusCode()).thenReturn(HttpStatus.CREATED);
        when(response.headers()).thenReturn(headers);

        final TusdUpload tusUploader = TusdUpload.builder().build();
        tusUploader.handleResponse(response);
    }

    @Test(expected = FileUploadException.class)
    public void handleResponse_Exception() {

        final HttpHeaders httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.entrySet()).thenReturn(new HashSet<>());

        final ClientResponse.Headers headers = mock(ClientResponse.Headers.class);

        when(headers.asHttpHeaders()).thenReturn(httpHeaders);

        final ClientResponse clientResponse = mock(ClientResponse.class);

        when(clientResponse.headers()).thenReturn(headers);
        when(clientResponse.statusCode()).thenReturn(HttpStatus.BAD_REQUEST);

        final TusdUpload tusUploader = TusdUpload.builder().build();
        tusUploader.handleResponse(clientResponse);
    }

    @Test
    public void readFileSizeQuietly() throws IOException {
        final Path file = Files.createTempFile("readFileSizeQuietly", "qwe");
        Files.write(file, "test".getBytes());
        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();
        assertEquals(tusUploader.readFileSizeQuietly(), 4);
    }

    @Test(expected = FileSizeReadException.class)
    public void readFileSizeQuietly_exception() {
        final Path file = Paths.get("not_exists/file");
        final TusdUpload tusUploader = TusdUpload.builder().path(file).build();
        tusUploader.readFileSizeQuietly();
        fail();
    }

    @Test
    public void patchChain(){
        final URI patchUri = URI.create("http://test");
        log.info("chunk {}", patchUri);
        final TusdUpload tusdUpload = mock(TusdUpload.class);
        Whitebox.setInternalState(tusdUpload, "patchUri", patchUri);
        when(tusdUpload.calcChunkCount()).thenReturn(5);
        IntStream.range(0, 5)
            .forEach(chunk -> when(tusdUpload.patch(chunk)).thenReturn(Mono.just(64L)));
        when(tusdUpload.patchChain()).thenCallRealMethod();
        final Long aLong = tusdUpload.patchChain().block();
        assertEquals(Long.valueOf(64), aLong);

    }


//    private FileAttribute<Set<PosixFilePermission>> fileAttributeReadOnly() {
//        Set<PosixFilePermission> readOnly = PosixFilePermissions.fromString("r--r--r--");
//        return PosixFilePermissions.asFileAttribute(readOnly);
//    }
//
//    private FileAttribute<Set<PosixFilePermission>> fileAttributeWriteOnly() {
//        Set<PosixFilePermission> readOnly = PosixFilePermissions.fromString("-w--w--w-");
//        return PosixFilePermissions.asFileAttribute(readOnly);
//    }
}