cmake_minimum_required(VERSION 3.4.1)

add_subdirectory(final_key)
add_subdirectory(argon2)

# NDK 21 does not align shared libraries for 16 KB devices by default.
# LINK_FLAGS also works with the project's minimum CMake version (3.4.1).
foreach(native_library final-key argon2)
    set_property(TARGET ${native_library} APPEND_STRING PROPERTY LINK_FLAGS
        " -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384")
endforeach()
