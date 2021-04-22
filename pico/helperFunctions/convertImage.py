import png # if that one is not yet installed: pip install pypng

infile = "240x135.png"
outfile_h = "../hid_printA/usb/background.hpp"

def color_to_bytes (color):
    r, g, b = color
    arr = bytearray(2)
    arr[0] = r & 0xF8
    arr[0] += (g & 0xE0) >> 5
    arr[1] = (g & 0x1C) << 3
    arr[1] += (b & 0xF8) >> 3
    return arr

def padhexa(s):
    return s[2:].zfill(4)

png_reader=png.Reader(infile)
image_data = png_reader.asRGBA8()

with open(outfile_h, "w") as file_h:
    print ("PNG file \nwidth {}\nheight {}\n".format(image_data[0], image_data[1]))
    lineCounter = 0
    file_h.write("#ifndef BACKGROUND_H_\n#define BACKGROUND_H_\n")
    file_h.write("uint16_t background_bmp[] = {\n")

    for row in image_data[2]:
        for r, g, b, a in zip(row[::4], row[1::4], row[2::4], row[3::4]):
            #print ("This pixel {:02x}{:02x}{:02x} {:02x}".format(r, g, b, a))
            # convert to (RGB565)
            img_bytes = color_to_bytes ((r,g,b))

            file_h.write("    0x")
            twoByteString = padhexa(hex(img_bytes[1]*256 + img_bytes[0]))
            file_h.write(twoByteString)
            if lineCounter == (240 * 135 - 1):
                file_h.write("\n")
            else:
                file_h.write(", \n")
            lineCounter = lineCounter + 1

    file_h.write("};\n#endif \n")

file_h.close()
