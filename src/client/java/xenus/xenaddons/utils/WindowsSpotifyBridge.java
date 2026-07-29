package xenus.xenaddons.utils;

import java.lang.foreign.Arena;
import java.lang.foreign.AddressLayout;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Direct Java bridge to Windows' Global System Media Transport Controls.
 *
 * <p>This is the Windows equivalent of the AppleScript integration used on
 * macOS. It deliberately does not launch PowerShell or any external process.</p>
 */
final class WindowsSpotifyBridge {
    private static final long CALL_TIMEOUT_MS = 3500L;
    private static final ExecutorService NATIVE_THREAD = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = Thread.ofPlatform().unstarted(runnable);
        thread.setName("XenAddons Spotify WinRT");
        thread.setDaemon(true);
        return thread;
    });

    private WindowsSpotifyBridge() {}

    static Snapshot read(boolean includeDetails) {
        return call(() -> Native.read(includeDetails), null);
    }

    static boolean control(SpotifyNowPlaying.ControlAction action) {
        return call(() -> Native.control(action), false);
    }

    static boolean setRepeat(boolean enabled) {
        return call(() -> Native.setRepeat(enabled), false);
    }

    static boolean setShuffle(boolean enabled) {
        return call(() -> Native.setShuffle(enabled), false);
    }

    static boolean setVolume(int percent) {
        int safePercent = Math.max(0, Math.min(100, percent));
        return call(() -> Native.setVolume(safePercent / 100.0f), false);
    }

    static boolean toggleLike() {
        return call(Native::toggleLike, false);
    }

    private static <T> T call(Callable<T> action, T fallback) {
        Future<T> future = NATIVE_THREAD.submit(action);
        try {
            return future.get(CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException ignored) {
            future.cancel(true);
        }
        return fallback;
    }

    record Snapshot(
        String source,
        String artist,
        String title,
        boolean playing,
        byte[] artwork,
        int progressSeconds,
        int durationSeconds
    ) {}

    private static final class Native {
        private static final int MAX_ARTWORK_BYTES = 8 * 1024 * 1024;
        private static final long ASYNC_TIMEOUT_MS = 2200L;

        private static final ValueLayout.OfByte BYTE = ValueLayout.JAVA_BYTE;
        private static final ValueLayout.OfShort SHORT =
            ValueLayout.JAVA_SHORT.withOrder(ByteOrder.LITTLE_ENDIAN);
        private static final ValueLayout.OfInt INT =
            ValueLayout.JAVA_INT.withOrder(ByteOrder.LITTLE_ENDIAN);
        private static final ValueLayout.OfLong LONG =
            ValueLayout.JAVA_LONG.withOrder(ByteOrder.LITTLE_ENDIAN);
        private static final AddressLayout ADDRESS = ValueLayout.ADDRESS;

        private static final Linker LINKER = Linker.nativeLinker();
        private static final SymbolLookup COMBASE =
            SymbolLookup.libraryLookup("combase.dll", Arena.global());
        private static final SymbolLookup OLE32 =
            SymbolLookup.libraryLookup("ole32.dll", Arena.global());
        private static final SymbolLookup USER32 =
            SymbolLookup.libraryLookup("user32.dll", Arena.global());

        private static final MethodHandle RO_INITIALIZE = downcall(
            "RoInitialize",
            FunctionDescriptor.of(INT, INT)
        );
        private static final MethodHandle RO_GET_ACTIVATION_FACTORY = downcall(
            "RoGetActivationFactory",
            FunctionDescriptor.of(INT, ADDRESS, ADDRESS, ADDRESS)
        );
        private static final MethodHandle WINDOWS_CREATE_STRING = downcall(
            "WindowsCreateString",
            FunctionDescriptor.of(INT, ADDRESS, INT, ADDRESS)
        );
        private static final MethodHandle WINDOWS_DELETE_STRING = downcall(
            "WindowsDeleteString",
            FunctionDescriptor.of(INT, ADDRESS)
        );
        private static final MethodHandle WINDOWS_GET_STRING_RAW_BUFFER = downcall(
            "WindowsGetStringRawBuffer",
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS)
        );
        private static final MethodHandle CO_CREATE_INSTANCE = downcall(
            OLE32,
            "CoCreateInstance",
            FunctionDescriptor.of(INT, ADDRESS, ADDRESS, INT, ADDRESS, ADDRESS)
        );
        private static final MethodHandle ENUM_WINDOWS = downcall(
            USER32,
            "EnumWindows",
            FunctionDescriptor.of(INT, ADDRESS, LONG)
        );
        private static final MethodHandle GET_WINDOW_THREAD_PROCESS_ID = downcall(
            USER32,
            "GetWindowThreadProcessId",
            FunctionDescriptor.of(INT, ADDRESS, ADDRESS)
        );
        private static final MethodHandle IS_WINDOW_VISIBLE = downcall(
            USER32,
            "IsWindowVisible",
            FunctionDescriptor.of(INT, ADDRESS)
        );
        private static final MethodHandle GET_FOREGROUND_WINDOW = downcall(
            USER32,
            "GetForegroundWindow",
            FunctionDescriptor.of(ADDRESS)
        );
        private static final MethodHandle SET_FOREGROUND_WINDOW = downcall(
            USER32,
            "SetForegroundWindow",
            FunctionDescriptor.of(INT, ADDRESS)
        );
        private static final MethodHandle SHOW_WINDOW = downcall(
            USER32,
            "ShowWindow",
            FunctionDescriptor.of(INT, ADDRESS, INT)
        );
        private static final MethodHandle KEYBD_EVENT = downcall(
            USER32,
            "keybd_event",
            FunctionDescriptor.ofVoid(BYTE, BYTE, INT, LONG)
        );

        private static final UUID IID_IASYNC_INFO =
            UUID.fromString("00000036-0000-0000-c000-000000000046");
        private static final UUID IID_SESSION_MANAGER_STATICS =
            UUID.fromString("2050c4ee-11a0-57de-aed7-c97c70338245");
        private static final UUID IID_RANDOM_ACCESS_STREAM =
            UUID.fromString("905a0fe1-bc53-11df-8c49-001e4fc686da");
        private static final UUID IID_BUFFER_FACTORY =
            UUID.fromString("71af914d-c10f-484b-bc50-14bc623b3a27");
        private static final UUID IID_BUFFER_BYTE_ACCESS =
            UUID.fromString("905a0fef-bc53-11df-8c49-001e4fc686da");
        private static final UUID CLSID_MM_DEVICE_ENUMERATOR =
            UUID.fromString("bcde0395-e52f-467c-8e3d-c4579291692e");
        private static final UUID IID_MM_DEVICE_ENUMERATOR =
            UUID.fromString("a95664d2-9614-4f35-a746-de8db63617e6");
        private static final UUID IID_AUDIO_SESSION_MANAGER_2 =
            UUID.fromString("77aa99a0-1bd6-484f-8bc7-2c654c9a9b6f");
        private static final UUID IID_AUDIO_SESSION_CONTROL_2 =
            UUID.fromString("bfb7ff88-7239-4fc9-8fa2-07c950be9c6d");
        private static final UUID IID_SIMPLE_AUDIO_VOLUME =
            UUID.fromString("87ce5498-68d6-44e5-9215-6da47ef883d8");

        private static final String SESSION_MANAGER_CLASS =
            "Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager";
        private static final String BUFFER_CLASS = "Windows.Storage.Streams.Buffer";

        private static final int RO_INIT_MULTITHREADED = 1;
        private static final int ASYNC_COMPLETED = 1;
        private static final int PLAYBACK_STATUS_PLAYING = 4;
        private static final int MEDIA_REPEAT_NONE = 0;
        private static final int MEDIA_REPEAT_LIST = 2;
        private static final int CLSCTX_ALL = 23;
        private static final int KEYEVENTF_KEYUP = 2;

        private static boolean initialized;
        private static String cachedArtworkTrack = "";
        private static byte[] cachedArtwork;
        private static MemorySegment enumeratedSpotifyWindow = MemorySegment.NULL;
        private static final MethodHandle ENUM_WINDOW_CALLBACK;

        static {
            try {
                ENUM_WINDOW_CALLBACK = MethodHandles.lookup().findStatic(
                    Native.class,
                    "visitWindow",
                    MethodType.methodType(int.class, MemorySegment.class, long.class)
                );
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private Native() {}

        private static Snapshot read(boolean includeDetails) throws Exception {
            ensureInitialized();
            try (Arena arena = Arena.ofConfined()) {
                MediaSession selectedSession = findMediaSession(arena);
                MemorySegment session = selectedSession == null
                    ? MemorySegment.NULL
                    : selectedSession.handle();
                if (isNull(session)) return null;

                MemorySegment media = MemorySegment.NULL;
                MemorySegment playback = MemorySegment.NULL;
                MemorySegment timeline = MemorySegment.NULL;
                try {
                    media = awaitPointer(callOutPointer(session, 7, arena), 8, arena);
                    if (isNull(media)) return null;

                    String title = readHStringProperty(media, 6, arena).trim();
                    String artist = readHStringProperty(media, 9, arena).trim();
                    if (title.isBlank() || artist.isBlank()) return null;

                    playback = callOutPointer(session, 9, arena);
                    boolean playing = readIntProperty(playback, 7, arena) == PLAYBACK_STATUS_PLAYING;

                    int progressSeconds = -1;
                    int durationSeconds = -1;
                    byte[] artwork = null;
                    if (includeDetails) {
                        timeline = callOutPointer(session, 8, arena);
                        long startTicks = readLongProperty(timeline, 6, arena);
                        long endTicks = readLongProperty(timeline, 7, arena);
                        long positionTicks = readLongProperty(timeline, 10, arena);
                        progressSeconds = ticksToSeconds(Math.max(0L, positionTicks - startTicks));
                        durationSeconds = ticksToSeconds(Math.max(0L, endTicks - startTicks));

                        String trackKey = artist + '\u0000' + title;
                        if (!trackKey.equals(cachedArtworkTrack)) {
                            cachedArtworkTrack = trackKey;
                            cachedArtwork = readArtwork(media, arena);
                        }
                        artwork = cachedArtwork;
                    }

                    return new Snapshot(
                        selectedSession.source(),
                        artist,
                        title,
                        playing,
                        artwork,
                        progressSeconds,
                        durationSeconds
                    );
                } finally {
                    release(timeline);
                    release(playback);
                    release(media);
                    release(session);
                }
            }
        }

        private static boolean control(SpotifyNowPlaying.ControlAction action) throws Exception {
            ensureInitialized();
            try (Arena arena = Arena.ofConfined()) {
                MediaSession selectedSession = findMediaSession(arena);
                MemorySegment session = selectedSession == null
                    ? MemorySegment.NULL
                    : selectedSession.handle();
                if (isNull(session)) return false;
                try {
                    int slot = switch (action) {
                        case PREVIOUS -> 17;
                        case PLAY_PAUSE -> 20;
                        case NEXT -> 16;
                    };
                    return awaitBoolean(callOutPointer(session, slot, arena), arena);
                } finally {
                    release(session);
                }
            }
        }

        private static boolean setRepeat(boolean enabled) throws Exception {
            ensureInitialized();
            try (Arena arena = Arena.ofConfined()) {
                MediaSession selectedSession = findMediaSession(arena);
                MemorySegment session = selectedSession == null
                    ? MemorySegment.NULL
                    : selectedSession.handle();
                if (isNull(session)) return false;
                try {
                    MemorySegment operation = callIntOutPointer(
                        session,
                        21,
                        enabled ? MEDIA_REPEAT_LIST : MEDIA_REPEAT_NONE,
                        arena
                    );
                    return awaitBoolean(operation, arena);
                } finally {
                    release(session);
                }
            }
        }

        private static boolean setShuffle(boolean enabled) throws Exception {
            ensureInitialized();
            try (Arena arena = Arena.ofConfined()) {
                MediaSession selectedSession = findMediaSession(arena);
                MemorySegment session = selectedSession == null
                    ? MemorySegment.NULL
                    : selectedSession.handle();
                if (isNull(session)) return false;
                try {
                    MemorySegment out = arena.allocate(ADDRESS);
                    check(
                        invokeInt(
                            function(session, 23),
                            FunctionDescriptor.of(INT, ADDRESS, BYTE, ADDRESS),
                            session,
                            enabled ? (byte) 1 : (byte) 0,
                            out
                        ),
                        "TryChangeShuffleActiveAsync"
                    );
                    return awaitBoolean(out.get(ADDRESS, 0), arena);
                } finally {
                    release(session);
                }
            }
        }

        private static boolean setVolume(float volume) throws Exception {
            ensureInitialized();
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment deviceEnumerator = coCreate(
                    CLSID_MM_DEVICE_ENUMERATOR,
                    IID_MM_DEVICE_ENUMERATOR,
                    arena
                );
                MemorySegment device = MemorySegment.NULL;
                MemorySegment sessionManager = MemorySegment.NULL;
                MemorySegment sessionEnumerator = MemorySegment.NULL;
                try {
                    device = callTwoIntOutPointer(deviceEnumerator, 4, 0, 1, arena);
                    sessionManager = activate(device, IID_AUDIO_SESSION_MANAGER_2, arena);
                    sessionEnumerator = callOutPointer(sessionManager, 5, arena);

                    int count = readIntProperty(sessionEnumerator, 3, arena);
                    boolean changed = false;
                    for (int index = 0; index < count; index++) {
                        MemorySegment sessionControl = callIntOutPointer(
                            sessionEnumerator,
                            4,
                            index,
                            arena
                        );
                        MemorySegment sessionControl2 = MemorySegment.NULL;
                        MemorySegment simpleVolume = MemorySegment.NULL;
                        try {
                            sessionControl2 = queryInterface(
                                sessionControl,
                                IID_AUDIO_SESSION_CONTROL_2,
                                arena
                            );
                            int pid = readIntProperty(sessionControl2, 14, arena);
                            if (!isSpotifyProcess(pid)) continue;

                            simpleVolume = queryInterface(
                                sessionControl,
                                IID_SIMPLE_AUDIO_VOLUME,
                                arena
                            );
                            check(
                                invokeInt(
                                    function(simpleVolume, 3),
                                    FunctionDescriptor.of(
                                        INT,
                                        ADDRESS,
                                        ValueLayout.JAVA_FLOAT,
                                        ADDRESS
                                    ),
                                    simpleVolume,
                                    volume,
                                    guid(arena, new UUID(0L, 0L))
                                ),
                                "ISimpleAudioVolume.SetMasterVolume"
                            );
                            changed = true;
                        } finally {
                            release(simpleVolume);
                            release(sessionControl2);
                            release(sessionControl);
                        }
                    }
                    return changed;
                } finally {
                    release(sessionEnumerator);
                    release(sessionManager);
                    release(device);
                    release(deviceEnumerator);
                }
            }
        }

        private static boolean toggleLike() throws Exception {
            ensureInitialized();
            MemorySegment spotifyWindow = findSpotifyWindow();
            if (isNull(spotifyWindow)) return false;

            MemorySegment previous = invokeAddress(GET_FOREGROUND_WINDOW);
            invokeInt(SHOW_WINDOW, spotifyWindow, 9);
            if (invokeInt(SET_FOREGROUND_WINDOW, spotifyWindow) == 0) return false;

            try {
                Thread.sleep(70L);
                keyEvent(0x12, false);
                keyEvent(0x10, false);
                keyEvent(0x42, false);
                keyEvent(0x42, true);
                keyEvent(0x10, true);
                keyEvent(0x12, true);
                Thread.sleep(70L);
                return true;
            } finally {
                if (!isNull(previous)) {
                    invokeInt(SET_FOREGROUND_WINDOW, previous);
                }
            }
        }

        private static MemorySegment coCreate(UUID classId, UUID interfaceId, Arena arena)
            throws Exception {
            MemorySegment out = arena.allocate(ADDRESS);
            check(
                invokeInt(
                    CO_CREATE_INSTANCE,
                    guid(arena, classId),
                    MemorySegment.NULL,
                    CLSCTX_ALL,
                    guid(arena, interfaceId),
                    out
                ),
                "CoCreateInstance"
            );
            return out.get(ADDRESS, 0);
        }

        private static MemorySegment activate(MemorySegment device, UUID iid, Arena arena)
            throws Exception {
            MemorySegment out = arena.allocate(ADDRESS);
            check(
                invokeInt(
                    function(device, 3),
                    FunctionDescriptor.of(INT, ADDRESS, ADDRESS, INT, ADDRESS, ADDRESS),
                    device,
                    guid(arena, iid),
                    CLSCTX_ALL,
                    MemorySegment.NULL,
                    out
                ),
                "IMMDevice.Activate"
            );
            return out.get(ADDRESS, 0);
        }

        private static MemorySegment findSpotifyWindow() throws Exception {
            enumeratedSpotifyWindow = MemorySegment.NULL;
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment callback = LINKER.upcallStub(
                    ENUM_WINDOW_CALLBACK,
                    FunctionDescriptor.of(INT, ADDRESS, LONG),
                    arena
                );
                invokeInt(ENUM_WINDOWS, callback, 0L);
            }
            return enumeratedSpotifyWindow;
        }

        @SuppressWarnings("unused")
        private static int visitWindow(MemorySegment window, long ignored) {
            try {
                if (invokeInt(IS_WINDOW_VISIBLE, window) == 0) return 1;
                try (Arena arena = Arena.ofConfined()) {
                    MemorySegment pidOut = arena.allocate(INT);
                    invokeInt(GET_WINDOW_THREAD_PROCESS_ID, window, pidOut);
                    if (!isSpotifyProcess(pidOut.get(INT, 0))) return 1;
                    enumeratedSpotifyWindow = window;
                    return 0;
                }
            } catch (Exception ignoredException) {
                return 1;
            }
        }

        private static boolean isSpotifyProcess(int pid) {
            if (pid <= 0) return false;
            Optional<ProcessHandle> process = ProcessHandle.of(Integer.toUnsignedLong(pid));
            if (process.isEmpty()) return false;
            String command = process.get().info().command().orElse("");
            if (command.isBlank()) return false;
            try {
                Path filename = Path.of(command).getFileName();
                return filename != null && "spotify.exe".equalsIgnoreCase(filename.toString());
            } catch (Exception ignored) {
                return false;
            }
        }

        private static void keyEvent(int virtualKey, boolean released) throws Exception {
            invokeVoid(
                KEYBD_EVENT,
                (byte) virtualKey,
                (byte) 0,
                released ? KEYEVENTF_KEYUP : 0,
                0L
            );
        }

        private static MediaSession findMediaSession(Arena arena) throws Exception {
            MemorySegment factory = activationFactory(
                SESSION_MANAGER_CLASS,
                IID_SESSION_MANAGER_STATICS,
                arena
            );
            MemorySegment manager = MemorySegment.NULL;
            MemorySegment sessions = MemorySegment.NULL;
            try {
                manager = awaitPointer(callOutPointer(factory, 6, arena), 8, arena);

                MemorySegment currentSession = callOutPointer(manager, 6, arena);
                if (!isNull(currentSession)) {
                    try {
                        String appId = readHStringProperty(currentSession, 6, arena);
                        return new MediaSession(currentSession, sourceName(appId));
                    } catch (Exception ignored) {
                        release(currentSession);
                    }
                }

                sessions = callOutPointer(manager, 7, arena);
                int count = readIntProperty(sessions, 7, arena);
                MediaSession fallback = null;

                for (int index = 0; index < count; index++) {
                    MemorySegment session = callIntOutPointer(sessions, 6, index, arena);
                    String appId;
                    try {
                        appId = readHStringProperty(session, 6, arena);
                    } catch (Exception ignored) {
                        release(session);
                        continue;
                    }

                    String source = sourceName(appId);
                    if ("Spotify".equals(source) || "Deezer".equals(source)) {
                        if (fallback != null) release(fallback.handle());
                        return new MediaSession(session, source);
                    }
                    if (fallback == null) {
                        fallback = new MediaSession(session, source);
                    } else {
                        release(session);
                    }
                }
                return fallback;
            } finally {
                release(sessions);
                release(manager);
                release(factory);
            }
        }

        private static String sourceName(String appId) {
            String normalized = appId == null ? "" : appId.toLowerCase(Locale.ROOT);
            if (normalized.contains("deezer")) return "Deezer";
            if (normalized.contains("spotify")) return "Spotify";
            return "Music";
        }

        private record MediaSession(MemorySegment handle, String source) {}

        private static byte[] readArtwork(MemorySegment media, Arena arena) {
            MemorySegment thumbnail = MemorySegment.NULL;
            MemorySegment stream = MemorySegment.NULL;
            MemorySegment randomAccessStream = MemorySegment.NULL;
            MemorySegment inputStream = MemorySegment.NULL;
            MemorySegment bufferFactory = MemorySegment.NULL;
            MemorySegment buffer = MemorySegment.NULL;
            MemorySegment resultBuffer = MemorySegment.NULL;
            MemorySegment byteAccess = MemorySegment.NULL;
            try {
                thumbnail = callOutPointer(media, 15, arena);
                if (isNull(thumbnail)) return null;

                stream = awaitPointer(callOutPointer(thumbnail, 6, arena), 8, arena);
                randomAccessStream = queryInterface(stream, IID_RANDOM_ACCESS_STREAM, arena);
                long size = readLongProperty(randomAccessStream, 6, arena);
                if (size <= 0L || size > MAX_ARTWORK_BYTES) return null;

                inputStream = callLongOutPointer(randomAccessStream, 8, 0L, arena);
                bufferFactory = activationFactory(BUFFER_CLASS, IID_BUFFER_FACTORY, arena);
                buffer = callIntOutPointer(bufferFactory, 6, (int) size, arena);

                MemorySegment readOut = arena.allocate(ADDRESS);
                check(
                    invokeInt(
                        function(inputStream, 6),
                        FunctionDescriptor.of(INT, ADDRESS, ADDRESS, INT, INT, ADDRESS),
                        inputStream,
                        buffer,
                        (int) size,
                        0,
                        readOut
                    ),
                    "IInputStream.ReadAsync"
                );
                resultBuffer = awaitPointer(readOut.get(ADDRESS, 0), 10, arena);
                int length = readIntProperty(resultBuffer, 7, arena);
                if (length <= 0 || length > MAX_ARTWORK_BYTES) return null;

                byteAccess = queryInterface(resultBuffer, IID_BUFFER_BYTE_ACCESS, arena);
                MemorySegment bytesOut = arena.allocate(ADDRESS);
                check(
                    invokeInt(
                        function(byteAccess, 3),
                        FunctionDescriptor.of(INT, ADDRESS, ADDRESS),
                        byteAccess,
                        bytesOut
                    ),
                    "IBufferByteAccess.Buffer"
                );
                MemorySegment bytes = bytesOut.get(ADDRESS, 0);
                return isNull(bytes) ? null : bytes.reinterpret(length).toArray(BYTE);
            } catch (Exception ignored) {
                return null;
            } finally {
                release(byteAccess);
                release(resultBuffer);
                release(buffer);
                release(bufferFactory);
                release(inputStream);
                release(randomAccessStream);
                release(stream);
                release(thumbnail);
            }
        }

        private static MemorySegment activationFactory(String className, UUID iid, Arena arena)
            throws Exception {
            MemorySegment hstring = createHString(className, arena);
            try {
                MemorySegment out = arena.allocate(ADDRESS);
                check(
                    invokeInt(
                        RO_GET_ACTIVATION_FACTORY,
                        hstring,
                        guid(arena, iid),
                        out
                    ),
                    "RoGetActivationFactory(" + className + ")"
                );
                return out.get(ADDRESS, 0);
            } finally {
                deleteHString(hstring);
            }
        }

        private static MemorySegment awaitPointer(
            MemorySegment operation,
            int resultSlot,
            Arena arena
        ) throws Exception {
            if (isNull(operation)) return MemorySegment.NULL;
            try {
                await(operation, arena);
                MemorySegment out = arena.allocate(ADDRESS);
                check(
                    invokeInt(
                        function(operation, resultSlot),
                        FunctionDescriptor.of(INT, ADDRESS, ADDRESS),
                        operation,
                        out
                    ),
                    "IAsyncOperation.GetResults"
                );
                return out.get(ADDRESS, 0);
            } finally {
                release(operation);
            }
        }

        private static boolean awaitBoolean(MemorySegment operation, Arena arena) throws Exception {
            if (isNull(operation)) return false;
            try {
                await(operation, arena);
                MemorySegment out = arena.allocate(BYTE);
                check(
                    invokeInt(
                        function(operation, 8),
                        FunctionDescriptor.of(INT, ADDRESS, ADDRESS),
                        operation,
                        out
                    ),
                    "IAsyncOperation<Boolean>.GetResults"
                );
                return out.get(BYTE, 0) != 0;
            } finally {
                release(operation);
            }
        }

        private static void await(MemorySegment operation, Arena arena) throws Exception {
            MemorySegment info = queryInterface(operation, IID_IASYNC_INFO, arena);
            try {
                long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(ASYNC_TIMEOUT_MS);
                while (System.nanoTime() < deadline) {
                    int status = readIntProperty(info, 7, arena);
                    if (status == ASYNC_COMPLETED) return;
                    if (status > ASYNC_COMPLETED) {
                        throw new IllegalStateException("WinRT async operation failed: " + status);
                    }
                    Thread.sleep(4L);
                }
                throw new TimeoutException("WinRT async operation timed out");
            } finally {
                release(info);
            }
        }

        private static MemorySegment queryInterface(MemorySegment object, UUID iid, Arena arena)
            throws Exception {
            MemorySegment out = arena.allocate(ADDRESS);
            check(
                invokeInt(
                    function(object, 0),
                    FunctionDescriptor.of(INT, ADDRESS, ADDRESS, ADDRESS),
                    object,
                    guid(arena, iid),
                    out
                ),
                "IUnknown.QueryInterface"
            );
            return out.get(ADDRESS, 0);
        }

        private static MemorySegment callOutPointer(MemorySegment object, int slot, Arena arena)
            throws Exception {
            MemorySegment out = arena.allocate(ADDRESS);
            check(
                invokeInt(
                    function(object, slot),
                    FunctionDescriptor.of(INT, ADDRESS, ADDRESS),
                    object,
                    out
                ),
                "COM slot " + slot
            );
            return out.get(ADDRESS, 0);
        }

        private static MemorySegment callIntOutPointer(
            MemorySegment object,
            int slot,
            int value,
            Arena arena
        ) throws Exception {
            MemorySegment out = arena.allocate(ADDRESS);
            check(
                invokeInt(
                    function(object, slot),
                    FunctionDescriptor.of(INT, ADDRESS, INT, ADDRESS),
                    object,
                    value,
                    out
                ),
                "COM slot " + slot
            );
            return out.get(ADDRESS, 0);
        }

        private static MemorySegment callLongOutPointer(
            MemorySegment object,
            int slot,
            long value,
            Arena arena
        ) throws Exception {
            MemorySegment out = arena.allocate(ADDRESS);
            check(
                invokeInt(
                    function(object, slot),
                    FunctionDescriptor.of(INT, ADDRESS, LONG, ADDRESS),
                    object,
                    value,
                    out
                ),
                "COM slot " + slot
            );
            return out.get(ADDRESS, 0);
        }

        private static MemorySegment callTwoIntOutPointer(
            MemorySegment object,
            int slot,
            int first,
            int second,
            Arena arena
        ) throws Exception {
            MemorySegment out = arena.allocate(ADDRESS);
            check(
                invokeInt(
                    function(object, slot),
                    FunctionDescriptor.of(INT, ADDRESS, INT, INT, ADDRESS),
                    object,
                    first,
                    second,
                    out
                ),
                "COM slot " + slot
            );
            return out.get(ADDRESS, 0);
        }

        private static int readIntProperty(MemorySegment object, int slot, Arena arena)
            throws Exception {
            MemorySegment out = arena.allocate(INT);
            check(
                invokeInt(
                    function(object, slot),
                    FunctionDescriptor.of(INT, ADDRESS, ADDRESS),
                    object,
                    out
                ),
                "COM integer property " + slot
            );
            return out.get(INT, 0);
        }

        private static long readLongProperty(MemorySegment object, int slot, Arena arena)
            throws Exception {
            MemorySegment out = arena.allocate(LONG);
            check(
                invokeInt(
                    function(object, slot),
                    FunctionDescriptor.of(INT, ADDRESS, ADDRESS),
                    object,
                    out
                ),
                "COM long property " + slot
            );
            return out.get(LONG, 0);
        }

        private static String readHStringProperty(MemorySegment object, int slot, Arena arena)
            throws Exception {
            MemorySegment out = arena.allocate(ADDRESS);
            check(
                invokeInt(
                    function(object, slot),
                    FunctionDescriptor.of(INT, ADDRESS, ADDRESS),
                    object,
                    out
                ),
                "COM string property " + slot
            );
            MemorySegment hstring = out.get(ADDRESS, 0);
            if (isNull(hstring)) return "";
            try {
                MemorySegment lengthOut = arena.allocate(INT);
                MemorySegment raw = invokeAddress(
                    WINDOWS_GET_STRING_RAW_BUFFER,
                    hstring,
                    lengthOut
                );
                int length = lengthOut.get(INT, 0);
                if (length <= 0 || isNull(raw)) return "";
                byte[] utf16 = raw.reinterpret((long) length * 2L).toArray(BYTE);
                return new String(utf16, StandardCharsets.UTF_16LE);
            } finally {
                deleteHString(hstring);
            }
        }

        private static MemorySegment createHString(String value, Arena arena) throws Exception {
            byte[] utf16 = value.getBytes(StandardCharsets.UTF_16LE);
            MemorySegment source = arena.allocate(utf16.length, 2L);
            MemorySegment.copy(utf16, 0, source, BYTE, 0, utf16.length);
            MemorySegment out = arena.allocate(ADDRESS);
            check(
                invokeInt(WINDOWS_CREATE_STRING, source, value.length(), out),
                "WindowsCreateString"
            );
            return out.get(ADDRESS, 0);
        }

        private static void deleteHString(MemorySegment value) {
            if (isNull(value)) return;
            try {
                invokeInt(WINDOWS_DELETE_STRING, value);
            } catch (Exception ignored) {}
        }

        private static MemorySegment guid(Arena arena, UUID uuid) {
            MemorySegment value = arena.allocate(16L, 4L);
            long most = uuid.getMostSignificantBits();
            long least = uuid.getLeastSignificantBits();
            value.set(INT, 0L, (int) (most >>> 32));
            value.set(SHORT, 4L, (short) (most >>> 16));
            value.set(SHORT, 6L, (short) most);
            for (int index = 0; index < 8; index++) {
                value.set(BYTE, 8L + index, (byte) (least >>> (56 - (index * 8))));
            }
            return value;
        }

        private static void ensureInitialized() throws Exception {
            if (initialized) return;
            check(invokeInt(RO_INITIALIZE, RO_INIT_MULTITHREADED), "RoInitialize");
            initialized = true;
        }

        private static int ticksToSeconds(long ticks) {
            long seconds = ticks / 10_000_000L;
            return seconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seconds;
        }

        private static MemorySegment function(MemorySegment object, int slot) {
            if (isNull(object)) throw new IllegalStateException("Null COM object");
            MemorySegment vtable = object.reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0);
            return vtable
                .reinterpret((long) (slot + 1) * ADDRESS.byteSize())
                .get(ADDRESS, (long) slot * ADDRESS.byteSize());
        }

        private static void release(MemorySegment object) {
            if (isNull(object)) return;
            try {
                LINKER.downcallHandle(
                    function(object, 2),
                    FunctionDescriptor.of(INT, ADDRESS)
                ).invoke(object);
            } catch (Throwable ignored) {}
        }

        private static boolean isNull(MemorySegment value) {
            return value == null || value.address() == 0L;
        }

        private static MethodHandle downcall(String symbol, FunctionDescriptor descriptor) {
            return downcall(COMBASE, symbol, descriptor);
        }

        private static MethodHandle downcall(
            SymbolLookup lookup,
            String symbol,
            FunctionDescriptor descriptor
        ) {
            return LINKER.downcallHandle(
                lookup.find(symbol).orElseThrow(
                    () -> new IllegalStateException("Missing native symbol " + symbol)
                ),
                descriptor
            );
        }

        private static int invokeInt(MethodHandle handle, Object... arguments) throws Exception {
            try {
                return (int) handle.invokeWithArguments(arguments);
            } catch (Throwable throwable) {
                throw asException(throwable);
            }
        }

        private static int invokeInt(
            MemorySegment address,
            FunctionDescriptor descriptor,
            Object... arguments
        ) throws Exception {
            try {
                return (int) LINKER
                    .downcallHandle(address, descriptor)
                    .invokeWithArguments(arguments);
            } catch (Throwable throwable) {
                throw asException(throwable);
            }
        }

        private static MemorySegment invokeAddress(MethodHandle handle, Object... arguments)
            throws Exception {
            try {
                return (MemorySegment) handle.invokeWithArguments(arguments);
            } catch (Throwable throwable) {
                throw asException(throwable);
            }
        }

        private static void invokeVoid(MethodHandle handle, Object... arguments)
            throws Exception {
            try {
                handle.invokeWithArguments(arguments);
            } catch (Throwable throwable) {
                throw asException(throwable);
            }
        }

        private static Exception asException(Throwable throwable) {
            return throwable instanceof Exception exception
                ? exception
                : new RuntimeException(throwable);
        }

        private static void check(int hresult, String operation) {
            if (hresult < 0) {
                throw new IllegalStateException(
                    operation + " failed with HRESULT 0x"
                        + Integer.toUnsignedString(hresult, 16)
                );
            }
        }
    }
}
