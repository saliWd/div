import png # if that one is not yet installed: pip install pypng

outfile_imgdata = "../hid_printA/usb/imgdata.hpp"
png_reader=png.Reader("240x135.png")
image_data_bg = png_reader.asRGBA8()
print ("PNG file. Width = {}, height = {}".format(image_data_bg[0], image_data_bg[1]))
png_reader=png.Reader("54x31_49.19.png")
image_data_stop = png_reader.asRGBA8()
print ("PNG file. Width = {}, height = {}".format(image_data_stop[0], image_data_stop[1]))


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


def writeHexVals(r,g,b,file_h,line_counter,lastElem):
    img_bytes = color_to_bytes ((r,g,b)) # convert to (RGB565)
    file_h.write("    0x")
    two_byte_string = padhexa(hex(img_bytes[1]*256 + img_bytes[0]))
    file_h.write(two_byte_string)
    if line_counter == (lastElem):
        file_h.write("\n")
    else:
        file_h.write(", \n")
    return line_counter + 1


with open(outfile_imgdata, "w") as file_h:
    file_h.write("#ifndef IMGDATA_H_\n#define IMGDATA_H_\n")

    line_counter = 0
    file_h.write("uint16_t background_bmp[] = {\n")
    for row in image_data_bg[2]:
        for r, g, b, a in zip(row[::4], row[1::4], row[2::4], row[3::4]):
            #print ("This pixel {:02x}{:02x}{:02x} {:02x}".format(r, g, b, a))
            line_counter = writeHexVals(r,g,b,file_h,line_counter,240*135-1)
    file_h.write("};\n")

    line_counter = 0
    file_h.write("uint16_t stop_bmp[] = {\n")
    for row in image_data_stop[2]:
        for r, g, b, a in zip(row[::4], row[1::4], row[2::4], row[3::4]):
            line_counter = writeHexVals(r,g,b,file_h,line_counter,54*31-1)
    file_h.write("};\n")

    file_h.write("#endif \n")
file_h.close()
