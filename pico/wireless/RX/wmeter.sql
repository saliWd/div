SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;


CREATE TABLE `wmeter` (
  `id` bigint(20) NOT NULL,
  `device` varchar(8) DEFAULT NULL,
  `consumption` decimal(10,3) NOT NULL,
  `consDiff` decimal(10,3) NOT NULL,
  `generation` decimal(10,3) NOT NULL,
  `genDiff` decimal(10,3) NOT NULL,
  `date` timestamp NOT NULL DEFAULT current_timestamp(),
  `dateDiff` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


ALTER TABLE `wmeter`
  ADD PRIMARY KEY (`id`);


ALTER TABLE `wmeter`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
